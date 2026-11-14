-- Keep V1 and its group history intact. New writes use direct course enrollment.
ALTER TABLE course_teachers ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;
CREATE TABLE course_enrollments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    student_account_id BIGINT NOT NULL CHECK (student_account_id > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','COMPLETED','LEFT')),
    enrolled_at TIMESTAMPTZ NOT NULL,
    UNIQUE (course_id, student_account_id)
);
INSERT INTO course_enrollments(id, course_id, student_account_id, status, enrolled_at)
SELECT gen_random_uuid(), g.course_id, e.student_account_id,
       CASE WHEN bool_or(e.status='ACTIVE' AND g.status='ACTIVE') THEN 'ACTIVE'
            WHEN bool_or(e.status='COMPLETED') THEN 'COMPLETED' ELSE 'LEFT' END,
       min(e.enrolled_at)
FROM enrollments e JOIN course_groups g ON g.id=e.group_id
GROUP BY g.course_id, e.student_account_id;
CREATE INDEX course_enrollments_student_idx ON course_enrollments(student_account_id);

CREATE TABLE student_points (
    id UUID PRIMARY KEY,
    enrollment_id UUID NOT NULL REFERENCES course_enrollments(id),
    request_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('AWARD','CORRECTION')),
    points INTEGER NOT NULL CHECK (points BETWEEN -1000 AND 1000 AND points <> 0),
    requested_points INTEGER NOT NULL CHECK (requested_points BETWEEN 0 AND 1000),
    reason VARCHAR(500) NOT NULL CHECK (length(trim(reason)) > 0),
    awarded_by_account_id BIGINT NOT NULL CHECK (awarded_by_account_id > 0),
    original_award_id UUID REFERENCES student_points(id),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (enrollment_id, request_id),
    CHECK ((type='AWARD' AND points>0 AND points=requested_points AND original_award_id IS NULL)
        OR (type='CORRECTION' AND original_award_id IS NOT NULL))
);
CREATE INDEX student_points_history_idx ON student_points(enrollment_id, created_at DESC, id);
CREATE INDEX student_points_original_idx ON student_points(original_award_id);

-- A published course never makes syllabus materials available to students.
UPDATE materials SET visibility='TEACHER_ONLY' WHERE visibility <> 'TEACHER_ONLY';
ALTER TABLE materials ADD CONSTRAINT materials_staff_only CHECK (visibility='TEACHER_ONLY');
ALTER TABLE materials DROP CONSTRAINT materials_state_check;
ALTER TABLE materials ADD CONSTRAINT materials_state_check
    CHECK (state IN ('PENDING','READY','FAILED','CLEANING','ARCHIVED'));
ALTER TABLE materials ADD COLUMN cleanup_started_at TIMESTAMPTZ;
