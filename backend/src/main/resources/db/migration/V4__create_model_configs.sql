create table model_configs (
    id uuid primary key,
    display_name varchar(100) not null,
    protocol varchar(32) not null,
    base_url varchar(2048) not null,
    model_id varchar(255) not null,
    capability_score integer not null check (capability_score between 1 and 10),
    description text,
    input_cost_micros_per_million_tokens bigint not null
        check (input_cost_micros_per_million_tokens >= 0),
    output_cost_micros_per_million_tokens bigint not null
        check (output_cost_micros_per_million_tokens >= 0),
    enabled boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_model_configs_enabled on model_configs(enabled);
