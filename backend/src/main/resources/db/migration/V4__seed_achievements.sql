-- V4__seed_achievements.sql
-- Seed danych dla tabeli achievement (9 osiagniec MVP)
-- UWAGA: Tej migracji nie modyfikujemy — nowe osiagniecia w V5 lub wyzej.

INSERT INTO achievement (code, name, name_pl, description, condition_type, condition_value) VALUES
  ('FIRST_CATCH',       'First Catch',        'Pierwszy zlap',         'Zlap swojego pierwszego psa',                    'total_catches',  1),
  ('TEN_CATCHES',       'Rookie Hunter',      'Poczatkujacy lowca',    'Zlap 10 psow',                                   'total_catches',  10),
  ('FIFTY_CATCHES',     'Seasoned Hunter',    'Doswiadczony lowca',    'Zlap 50 psow',                                   'total_catches',  50),
  ('HUNDRED_CATCHES',   'Master Hunter',      'Mistrz lowow',          'Zlap 100 psow',                                  'total_catches',  100),
  ('EXPLORER',          'Breed Explorer',     'Odkrywca ras',          'Odkryj 5 roznych ras',                           'unique_breeds',  5),
  ('BREED_COLLECTOR',   'Breed Collector',    'Kolekcjoner ras',       'Odkryj 20 roznych ras',                          'unique_breeds',  20),
  ('RARE_HUNTER',       'Rare Hunter',        'Lowca rzadkosci',       'Zlap psa o rzadkosci 4 lub wyzej',              'rare_catch',     4),
  ('SOCIAL',            'Social Pup',         'Towarzyski',            'Zdobadz 5 znajomych',                            'friends_count',  5),
  ('WEEK_STREAK',       'Week Warrior',       'Tygodniowy wojownik',   'Lapaj psy przez 7 kolejnych dni z rzedu',       'streak',         7);
