create table practice_projects (
  id varchar(36) primary key,
  title varchar(120) not null,
  goal varchar(1000) not null,
  template_id varchar(64) not null,
  input_revision integer not null,
  metrics_json longtext not null,
  process_note varchar(5000) not null,
  decision_origin_json longtext,
  created_at timestamp not null,
  updated_at timestamp not null
);

create table practice_criteria (
  id varchar(36) primary key,
  project_id varchar(36) not null,
  title varchar(120) not null,
  standard varchar(1000) not null,
  expected_evidence varchar(1000) not null,
  required boolean not null,
  order_index integer not null,
  constraint fk_practice_criterion_project foreign key (project_id) references practice_projects(id)
);

create table practice_evidence (
  id varchar(36) primary key,
  project_id varchar(36) not null,
  revision integer not null,
  title varchar(120) not null,
  kind varchar(16) not null,
  content longtext not null,
  criterion_ids_json longtext not null,
  updated_at timestamp not null,
  constraint fk_practice_evidence_project foreign key (project_id) references practice_projects(id)
);

create table practice_reviews (
  id varchar(36) primary key,
  project_id varchar(36) not null,
  review_number integer not null,
  input_revision integer not null,
  execution_status varchar(16) not null,
  snapshot_json longtext not null,
  result_json longtext,
  confirmation_json longtext,
  source varchar(8),
  provider_notice varchar(1000),
  request_id varchar(36) not null,
  request_hash varchar(64) not null,
  attempt_id varchar(36) not null,
  started_at timestamp not null,
  finished_at timestamp,
  constraint fk_practice_review_project foreign key (project_id) references practice_projects(id),
  constraint uq_practice_review_number unique (project_id, review_number),
  constraint uq_practice_review_request unique (request_id)
);

create table practice_feedback (
  id varchar(36) primary key,
  project_id varchar(36) not null,
  review_id varchar(36) not null,
  target varchar(16) not null,
  target_key varchar(120) not null,
  payload_json longtext not null,
  request_id varchar(36) not null,
  request_hash varchar(64) not null,
  item_index integer not null,
  confirmed_at timestamp not null,
  constraint fk_practice_feedback_project foreign key (project_id) references practice_projects(id),
  constraint fk_practice_feedback_review foreign key (review_id) references practice_reviews(id),
  constraint uq_practice_feedback_target unique (review_id, target, target_key),
  constraint uq_practice_feedback_request unique (request_id, item_index)
);

create index idx_practice_projects_updated on practice_projects(updated_at);
create index idx_practice_reviews_project on practice_reviews(project_id, review_number);
create index idx_practice_feedback_project on practice_feedback(project_id, confirmed_at);
