alter table orchestration_runs
    rename column budget_cents to budget_micros;

alter table orchestration_runs
    alter column budget_micros set not null;
