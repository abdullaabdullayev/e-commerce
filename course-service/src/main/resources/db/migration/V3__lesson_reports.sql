CREATE TABLE lesson_sessions (
    id UUID PRIMARY KEY,
    lesson_id UUID NOT NULL REFERENCES lessons(id),
    teacher_account_id BIGINT NOT NULL,
    held_at TIMESTAMPTZ NOT NULL,
    topic VARCHAR(160) NOT NULL,
    UNIQUE (lesson_id, teacher_account_id, held_at)
);
CREATE TABLE student_lesson_reports (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES lesson_sessions(id),
    enrollment_id UUID NOT NULL REFERENCES course_enrollments(id),
    attended BOOLEAN NOT NULL,
    learned VARCHAR(2000) NOT NULL,
    participation VARCHAR(2000) NOT NULL,
    recommendation VARCHAR(1000) NOT NULL,
    published_at TIMESTAMPTZ,
    UNIQUE (session_id, enrollment_id),
    CHECK (published_at IS NULL OR NOT attended OR
        (length(trim(learned)) > 0 AND length(trim(participation)) > 0))
);
CREATE INDEX reports_by_enrollment ON student_lesson_reports(enrollment_id, published_at DESC);
