-- https://www.postgresql.org/docs/10/sql-createtable.html
-- https://www.postgresql.org/docs/10/sql-droptable.html
-- https://www.postgresql.org/docs/10/datatype.html


DROP TABLE IF EXISTS venues;
CREATE TABLE venues (
  id integer NOT NULL,
  name varchar(255) NOT NULL,
  past_names varchar(255),
  latitude numeric(9, 4),
  longitude numeric(9, 4),
  slug varchar(255),
  location varchar(255),
  city varchar(255),
  state varchar(255),
  country varchar(255)
);

DROP TABLE IF EXISTS shows;
CREATE TABLE shows (
  id integer NOT NULL,
  show_date date NOT NULL,
  dow varchar(5) not null,
  month integer not null,
  year integer not null,
  era varchar(5),
  venue_id integer NOT NULL,
  duration integer,
  jamcharts boolean,
  sbd boolean,
  taper_notes text
);

DROP TABLE IF EXISTS songs;
CREATE TABLE songs (
  id integer NOT NULL,
  title varchar(255) NOT NULL,
  alias varchar(255) DEFAULT NULL,
  alias_slug varchar(255) DEFAULT NULL,
  slug varchar(255) NOT NULL
);

DROP TABLE IF EXISTS tracks;
CREATE TABLE tracks (
  id integer default NULL,
  show_id integer NOT NULL,
  song_ids integer[],
  venue_id integer NOT NULL,
  show_date date NOT NULL,
  dow varchar(5) not null,
  month integer not null,
  year integer not null,
  era varchar(5),
  title varchar(255) NOT NULL,
  position integer,
  duration integer,
  slug varchar(255),
  mp3 varchar(255),
  jamcharts boolean,
  sbd boolean,
  seguein boolean default false,
  segueout boolean default false,
  set varchar(5)
);
