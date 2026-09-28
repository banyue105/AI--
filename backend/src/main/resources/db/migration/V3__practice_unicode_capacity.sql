-- Keep the public code-point limits in PracticeRules. H2 VARCHAR counts UTF-16
-- units, so supplementary characters need twice the physical storage capacity.
-- This migration preserves databases which have already applied V2.
alter table practice_projects modify column title varchar(240) not null;
alter table practice_projects modify column goal varchar(2000) not null;
alter table practice_projects modify column process_note varchar(10000) not null;
alter table practice_criteria modify column title varchar(240) not null;
alter table practice_criteria modify column standard varchar(2000) not null;
alter table practice_criteria modify column expected_evidence varchar(2000) not null;
alter table practice_evidence modify column title varchar(240) not null;
alter table practice_feedback modify column target_key varchar(240) not null;
