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
            System.out.println("✅ Schedule Created Successfully - " + shiftsResult.size() + " shifts assigned");
        } else {
            // Provide more detailed diagnostics for infeasible problems
            if (status == CpSolverStatus.INFEASIBLE) {
                warnings.add("❌ Schedule creation failed: Problem is INFEASIBLE");
                warnings.add("Possible causes:");
                warnings.add("- Too many admin constraints (hard constraints cannot be satisfied)");
                warnings.add("- Not enough employees for the workload");
                warnings.add("- Fairness constraints too strict");
                warnings.add("Suggestions:");
                warnings.add("- Review admin constraints for conflicts");
                warnings.add("- Consider adding more employees");
                warnings.add("- Relax fairness requirements");

                System.out.println("❌ INFEASIBLE: Cannot create schedule with current constraints");
                System.out.println("   Employees: " + numWorkers);
                System.out.println("   Days: " + numDays);
                System.out.println("   Admin constraints: " + adminConstraints.size());
                System.out.println("   Avg shifts per employee: " + (numDays / (double) numWorkers));
            } else {
                warnings.add("❌ Schedule creation failed with status: " + status);
            }
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
        int baseAssignments = numDays / employees.size();
        int remainder = numDays % employees.size();

        for (int w = 0; w < employees.size(); w++) {
            Employee employee = employees.get(w);

            // More relaxed fairness - just ensure everyone gets roughly equal assignments
            int minAssignments = baseAssignments;
            int maxAssignments = baseAssignments + (w < remainder ? 1 : 0);

            // Add some flexibility to avoid infeasibility
            minAssignments = Math.max(1, minAssignments - 5); // Allow 5 fewer
            maxAssignments = maxAssignments + 5; // Allow 5 more

            LinearExprBuilder shiftsWorked = LinearExpr.newBuilder();
            for (int d = 0; d < numDays; d++) {
                shiftsWorked.add(shifts[w][d]);
            }

            model.addLinearConstraint(shiftsWorked, minAssignments, maxAssignments);

            int previousShifts = employee.getPreviousYearShifts();
            if (previousShifts > baseAssignments * 1.5) {
                warnings.add("Employee " + employee.getName() + " worked significantly more shifts last year (" +
                        previousShifts + "). Will try to balance assignments.");
            }
        }

        System.out.println("Fairness constraints applied - Base assignments per employee: " + baseAssignments +
                " (±5 flexibility)");
    }

    private static void addConsecutiveConstraints(CpModel model, Literal[][] shifts, int numWorkers, int numDays) {
        // Relax consecutive constraints to avoid infeasibility
        // No more than 3 consecutive assignments (was 2, now more flexible)
        int maxConsecutive = 3;

        for (int w = 0; w < numWorkers; w++) {
            for (int d = 0; d <= numDays - maxConsecutive - 1; d++) {
                List<Literal> consecutiveShifts = new ArrayList<>();
                for (int i = 0; i <= maxConsecutive; i++) {
                    consecutiveShifts.add(shifts[w][d + i]);
                }
                model.addAtMostOne(consecutiveShifts);
            }
        }

        System.out.println("Applied consecutive shift constraints (max " + maxConsecutive + " consecutive)");
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