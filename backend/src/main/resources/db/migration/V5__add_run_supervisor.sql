-- Existing runs have no known supervisor; preserve them with NULL.
alter table orchestration_runs
    add column supervisor_config_id uuid references model_configs(id);

create index idx_runs_supervisor_config_id on orchestration_runs(supervisor_config_id);
