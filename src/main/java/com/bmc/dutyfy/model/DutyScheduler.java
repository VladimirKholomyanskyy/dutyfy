package com.bmc.dutyfy.model;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DutyScheduler {

    public static SchedulingResult scheduleDuties(List<Employee> employees, LocalDate startDate, LocalDate endDate,
                                                  List<AdminConstraint> adminConstraints, List<LocalDate> holidays) {
        Loader.loadNativeLibraries();

        int numDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int numWorkers = employees.size();
        List<Shift> shiftsResult = new LinkedList<>();
        List<String> warnings = new ArrayList<>();

        if (numWorkers == 0) {
            return new SchedulingResult(false, shiftsResult, Arrays.asList("No active employees found"));
        }

        CpModel model = new CpModel();

        // Decision variables: x[w][d] = 1 if worker w is assigned on day d
        Literal[][] shifts = new Literal[numWorkers][numDays];
        for (int w = 0; w < numWorkers; w++) {
            for (int d = 0; d < numDays; d++) {
                shifts[w][d] = model.newBoolVar("shifts_" + w + "_" + d);
            }
        }

        // Constraint 1: Each day must be assigned to exactly one worker
        for (int d = 0; d < numDays; d++) {
            List<Literal> dailyAssignments = new ArrayList<>();
            for (int w = 0; w < numWorkers; w++) {
                dailyAssignments.add(shifts[w][d]);
            }
            model.addExactlyOne(dailyAssignments);
        }

        // Constraint 2: Admin constraints (hard constraints)
        addAdminConstraints(model, shifts, employees, adminConstraints, startDate, numDays, warnings);

        // Constraint 3: Assignments should be evenly distributed considering previous year
        addFairnessConstraints(model, shifts, employees, numDays, warnings);

        // Constraint 4: No consecutive assignments (configurable window)
        addConsecutiveConstraints(model, shifts, numWorkers, numDays);

        // Objective: Minimize violations of preferred off dates and balance holiday assignments
        LinearExprBuilder obj = LinearExpr.newBuilder();
        addObjectiveTerms(obj, shifts, employees, startDate, numDays, holidays);
        model.minimize(obj);

        // Solve the model
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(30.0); // Set time limit
        CpSolverStatus status = solver.solve(model);

        boolean success = false;
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            success = true;
            for (int d = 0; d < numDays; d++) {
                for (int w = 0; w < numWorkers; w++) {
                    if (solver.booleanValue(shifts[w][d])) {
                        shiftsResult.add(new Shift(employees.get(w), startDate.plusDays(d)));
                    }
                }
            }
            System.out.println("✅ Schedule Created Successfully");
        } else {
            warnings.add("❌ No feasible solution found. Check constraints and employee availability.");
            System.out.println("❌ No solution found: " + status);
        }

        return new SchedulingResult(success, shiftsResult, warnings);
    }

    private static void addAdminConstraints(CpModel model, Literal[][] shifts, List<Employee> employees,
                                            List<AdminConstraint> adminConstraints, LocalDate startDate,
                                            int numDays, List<String> warnings) {
        for (AdminConstraint constraint : adminConstraints) {
            int workerIndex = employees.indexOf(constraint.getEmployee());
            if (workerIndex >= 0) {
                int dayIndex = (int) ChronoUnit.DAYS.between(startDate, constraint.getConstraintDate());
                if (dayIndex >= 0 && dayIndex < numDays) {
                    // Hard constraint - this worker cannot work this day
                    model.addEquality(shifts[workerIndex][dayIndex], 0);
                    System.out.println("Applied admin constraint: " + constraint.getEmployee().getName() +
                            " cannot work on " + constraint.getConstraintDate() +
                            " (Reason: " + constraint.getReason() + ")");
                }
            }
        }
    }

    private static void addFairnessConstraints(CpModel model, Literal[][] shifts, List<Employee> employees,
                                               int numDays, List<String> warnings) {
        // Calculate target assignments considering previous year
        int totalAssignments = numDays;
        int totalPreviousShifts = employees.stream().mapToInt(e -> e.getPreviousYearShifts()).sum();

        for (int w = 0; w < employees.size(); w++) {
            Employee employee = employees.get(w);

            // Calculate fair share: fewer assignments for those who worked more last year
            int previousShifts = employee.getPreviousYearShifts();
            double adjustmentFactor = totalPreviousShifts > 0 ?
                    1.0 - (double) previousShifts / (totalPreviousShifts * 2.0) : 1.0;

            int baseAssignments = numDays / employees.size();
            int targetAssignments = Math.max(1, (int) (baseAssignments * adjustmentFactor));
            int maxAssignments = targetAssignments + (numDays % employees.size() > w ? 1 : 0);

            LinearExprBuilder shiftsWorked = LinearExpr.newBuilder();
            for (int d = 0; d < numDays; d++) {
                shiftsWorked.add(shifts[w][d]);
            }

            model.addLinearConstraint(shiftsWorked, Math.max(1, targetAssignments - 1), maxAssignments + 1);

            if (previousShifts > baseAssignments * 1.5) {
                warnings.add("Employee " + employee.getName() + " worked significantly more shifts last year (" +
                        previousShifts + "). Adjusting assignment to be more fair.");
            }
        }
    }

    private static void addConsecutiveConstraints(CpModel model, Literal[][] shifts, int numWorkers, int numDays) {
        // No more than 2 consecutive assignments (configurable)
        int maxConsecutive = 2;

        for (int w = 0; w < numWorkers; w++) {
            for (int d = 0; d <= numDays - maxConsecutive - 1; d++) {
                List<Literal> consecutiveShifts = new ArrayList<>();
                for (int i = 0; i <= maxConsecutive; i++) {
                    consecutiveShifts.add(shifts[w][d + i]);
                }
                model.addAtMostOne(consecutiveShifts);
            }
        }
    }

    private static void addObjectiveTerms(LinearExprBuilder obj, Literal[][] shifts, List<Employee> employees,
                                          LocalDate startDate, int numDays, List<LocalDate> holidays) {
        for (int w = 0; w < employees.size(); w++) {
            Employee worker = employees.get(w);
            for (int d = 0; d < numDays; d++) {
                LocalDate date = startDate.plusDays(d);
                long penalty = 0;

                // High penalty for preferred off dates
                if (worker.getOffDates().stream().anyMatch(offDate -> offDate.getOffDate().equals(date))) {
                    penalty += 100;
                }

                // Medium penalty for weekends to distribute fairly
                if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    penalty += 10;
                }

                // High penalty for holidays to distribute fairly
                if (holidays.contains(date)) {
                    penalty += 50;
                }

                // Small penalty based on previous year assignments (more = higher penalty)
                penalty += worker.getPreviousYearShifts() / 10;

                if (penalty > 0) {
                    obj.addTerm(shifts[w][d], penalty);
                }
            }
        }
    }

    public static class SchedulingResult {
        private final boolean success;
        private final List<Shift> shifts;
        private final List<String> warnings;

        public SchedulingResult(boolean success, List<Shift> shifts, List<String> warnings) {
            this.success = success;
            this.shifts = shifts;
            this.warnings = warnings;
        }

        public boolean isSuccess() {
            return success;
        }

        public List<Shift> getShifts() {
            return shifts;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}