create table orchestration_runs (
    id uuid primary key,
    objective text not null,
    status varchar(32) not null,
    budget_cents bigint,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table orchestration_tasks (
    id uuid primary key,
    run_id uuid not null references orchestration_runs(id),
    parent_task_id uuid references orchestration_tasks(id),
    title varchar(255) not null,
    status varchar(32) not null,
    model_tier varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_orchestration_tasks_run_id on orchestration_tasks(run_id);
create index idx_orchestration_tasks_parent_task_id on orchestration_tasks(parent_task_id);

