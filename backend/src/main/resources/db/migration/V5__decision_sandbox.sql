create table decision_scenarios (
  id varchar(36) primary key,
  title varchar(120) not null,
  goal varchar(500) not null,
  time_limit_days integer not null,
  budget_yuan decimal(14, 2) not null,
  people_count integer not null,
  has_server boolean not null,
  change_request varchar(500),
  revision integer not null,
  created_at timestamp not null,
  updated_at timestamp not null
);

create table decision_constraints (
  id varchar(255) primary key,
  scenario_id varchar(36) not null,
  type varchar(32) not null,
  label varchar(120) not null,
  constraint_value varchar(255) not null,
  source varchar(16) not null,
  confidence decimal(4, 3) not null,
  assumption varchar(500),
  constraint fk_decision_constraint_scenario foreign key (scenario_id) references decision_scenarios(id)
);

create table decision_resources (
  id varchar(255) primary key,
  scenario_id varchar(36) not null,
  type varchar(32) not null,
  label varchar(120) not null,
  quantity integer,
  unit varchar(32),
  source varchar(16) not null,
  confidence decimal(4, 3) not null,
  assumption varchar(500),
  constraint fk_decision_resource_scenario foreign key (scenario_id) references decision_scenarios(id)
);

create table decision_nodes (
  id varchar(255) primary key,
  scenario_id varchar(36) not null,
  type varchar(32) not null,
  label varchar(120) not null,
  source varchar(16) not null,
  confidence decimal(4, 3) not null,
  assumption varchar(500),
  constraint fk_decision_node_scenario foreign key (scenario_id) references decision_scenarios(id)
);

create table decision_relations (
  id varchar(255) primary key,
  scenario_id varchar(36) not null,
  from_node_id varchar(255) not null,
  to_node_id varchar(255) not null,
  label varchar(120) not null,
  source varchar(16) not null,
  confidence decimal(4, 3) not null,
  assumption varchar(500),
  constraint fk_decision_relation_scenario foreign key (scenario_id) references decision_scenarios(id)
);

create table decision_versions (
  id varchar(36) primary key,
  scenario_id varchar(36) not null,
  version_number integer not null,
  scenario_revision integer not null,
  change_summary varchar(500) not null,
  snapshot_json text not null,
  created_at timestamp not null,
  constraint fk_decision_version_scenario foreign key (scenario_id) references decision_scenarios(id),
  constraint uq_decision_version_number unique (scenario_id, version_number)
);

create table decision_simulations (
  id varchar(36) primary key,
  version_id varchar(36) not null,
  option_key varchar(16) not null,
  result_json text not null,
  constraint fk_decision_simulation_version foreign key (version_id) references decision_versions(id),
  constraint uq_decision_simulation_option unique (version_id, option_key)
);

create index idx_decision_versions_scenario on decision_versions(scenario_id, version_number);
