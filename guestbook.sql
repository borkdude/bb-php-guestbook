-- Table: public.guestbook

-- DROP TABLE public.guestbook;

CREATE TABLE public.guestbook
(
name text COLLATE pg_catalog."default",
message text COLLATE pg_catalog."default",
_created timestamp without time zone,
session text COLLATE pg_catalog."default",
CONSTRAINT session_unique UNIQUE (session)
)

TABLESPACE pg_default;

ALTER TABLE public.guestbook
OWNER to postgres;

GRANT ALL ON TABLE public.guestbook TO guestbook;

GRANT ALL ON TABLE public.guestbook TO postgres;
