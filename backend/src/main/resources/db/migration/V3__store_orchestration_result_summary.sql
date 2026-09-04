alter table orchestration_runs
    add column spent_micros bigint not null default 0;

alter table orchestration_runs
    add column plan_content text;

alter table orchestration_runs
    add column work_content text;

alter table orchestration_runs
    add column final_result text;
