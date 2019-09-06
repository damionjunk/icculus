-- :name find-song-by-id :?
-- :doc  Grab song matching the ID
SELECT * FROM songs WHERE id = :id

-- :name find-song-by-slug :?
SELECT * FROM songs WHERE slug = :slug OR alias_slug = :slug

-- :name first-played-by-id :?
-- :doc Finds the first time a song was played.
select t.id, t.show_date, t.era, t.title, t.position, t.duration, t.mp3, t.jamcharts, t.set, v.name, v.location, v.city, v.state, v.country from tracks t, venues v where v.id = t.venue_id AND :song_id = ANY (t.song_ids) order by t.show_date ASC limit 1

-- :name last-played-by-id :?
-- :doc Finds the last time a song was played.
select t.id, t.show_date, t.era, t.title, t.position, t.duration, t.mp3, t.jamcharts, t.set, v.name, v.location, v.city, v.state, v.country from tracks t, venues v where v.id = t.venue_id AND :song_id = ANY (t.song_ids) order by t.show_date DESC limit 1

-- :name last-n-played-by-id :?
-- :doc Finds the last time a song was played, with a :limit for last N.
select t.id, t.show_date, t.era, t.title, t.position, t.duration, t.mp3, t.jamcharts, t.set, v.name, v.location, v.city, v.state, v.country from tracks t, venues v where v.id = t.venue_id AND :song_id = ANY (t.song_ids) order by t.show_date DESC limit :limit

-- :name show-by-date :?
select s.id, s.show_date, s.venue_id, s.duration, v.name, v.location, v.city, v.state, v.country from shows s, venues v where v.id = s.venue_id AND s.show_date = date(:show_date)

-- :name tracks-by-show-id :?
select * from tracks where show_id = :show_id order by position;

-- :name shows-after-date :?
select count(*) from shows where show_date > :show_date

-- :name total-in-era :?
select count(id) as count from tracks where era = :era

-- :name total-in-range :?
select count(id) as count from tracks where show_date >= :start AND show_date < :end

-- :name track-total-by-era :?
select count(title) as plays from tracks where era = :era AND :song_id = ANY (song_ids) group by (title);

-- :name track-total-by-range :?
select count(title) as plays from tracks where show_date >= :start AND show_date < :end AND :song_id = ANY (song_ids) group by (title)

-- :name all-tracks-stats :?
select avg(duration), stddev_pop(duration) as sd, min(duration), max(duration) from tracks where :song_id = ANY (song_ids)

-- :name longest-by-song-id :?
select * from tracks where :song_id = ANY (song_ids) order by duration DESC limit :limit

-- :name shortest-by-song-id :?
select * from tracks where :song_id = ANY (song_ids) order by duration ASC limit :limit