CREATE
OR REPLACE FUNCTION jsonb_array_contains_ci(arr jsonb, needle text)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $$
SELECT EXISTS (SELECT 1
               FROM jsonb_array_elements_text(arr) AS element(value)
               WHERE value ILIKE '%' || needle || '%');
$$;
