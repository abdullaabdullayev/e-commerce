ALTER TABLE account_roles DROP CONSTRAINT account_roles_role_check;
ALTER TABLE account_roles ADD CONSTRAINT account_roles_role_check
    CHECK (role IN ('CUSTOMER', 'ADMIN', 'TEACHER', 'STUDENT', 'PARENT'));
