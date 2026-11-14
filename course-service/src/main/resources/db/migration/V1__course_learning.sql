CREATE TABLE courses (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    level VARCHAR(80) NOT NULL,
    min_age INTEGER NOT NULL CHECK (min_age BETWEEN 3 AND 100),
    max_age INTEGER NOT NULL CHECK (max_age BETWEEN min_age AND 100),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE course_teachers (
    course_id UUID NOT NULL REFERENCES courses(id),
    account_id BIGINT NOT NULL CHECK (account_id > 0),
    permission VARCHAR(10) NOT NULL CHECK (permission IN ('VIEW','EDIT')),
    PRIMARY KEY (course_id, account_id)
);
CREATE TABLE lessons (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    position INTEGER NOT NULL CHECK (position > 0),
    title VARCHAR(160) NOT NULL,
    objective TEXT NOT NULL DEFAULT '',
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 1 AND 600),
    teacher_notes TEXT NOT NULL DEFAULT '',
    archived BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (id, course_id),
    CONSTRAINT lessons_course_position UNIQUE (course_id, position) DEFERRABLE INITIALLY IMMEDIATE
);
CREATE TABLE course_groups (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    teacher_account_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ARCHIVED')),
    UNIQUE (id, course_id),
    FOREIGN KEY (course_id, teacher_account_id) REFERENCES course_teachers(course_id, account_id)
);
CREATE TABLE enrollments (
    group_id UUID NOT NULL REFERENCES course_groups(id),
    student_account_id BIGINT NOT NULL CHECK (student_account_id > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','COMPLETED','LEFT')),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, student_account_id)
);
CREATE TABLE group_lessons (
    group_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    course_id UUID NOT NULL,
    opened BOOLEAN NOT NULL DEFAULT false,
    opened_at TIMESTAMPTZ,
    PRIMARY KEY (group_id, lesson_id),
    FOREIGN KEY (group_id, course_id) REFERENCES course_groups(id, course_id),
    FOREIGN KEY (lesson_id, course_id) REFERENCES lessons(id, course_id)
);
CREATE TABLE materials (
    id UUID PRIMARY KEY,
    lesson_id UUID NOT NULL REFERENCES lessons(id),
    uploader_account_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('FILE','LINK')),
    visibility VARCHAR(20) NOT NULL DEFAULT 'TEACHER_ONLY' CHECK (visibility IN ('TEACHER_ONLY','STUDENT_VISIBLE')),
    state VARCHAR(20) NOT NULL CHECK (state IN ('PENDING','READY','FAILED','ARCHIVED')),
    object_key VARCHAR(200),
    filename VARCHAR(200),
    content_type VARCHAR(100),
    size_bytes BIGINT CHECK (size_bytes > 0 AND size_bytes <= 26214400),
    url VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((kind='FILE' AND object_key IS NOT NULL AND filename IS NOT NULL AND size_bytes IS NOT NULL)
        OR (kind='LINK' AND url IS NOT NULL AND object_key IS NULL))
);
CREATE INDEX course_teachers_account_idx ON course_teachers(account_id);
CREATE INDEX course_groups_teacher_idx ON course_groups(teacher_account_id);
CREATE INDEX enrollments_student_idx ON enrollments(student_account_id, status);
CREATE INDEX materials_lesson_idx ON materials(lesson_id);
