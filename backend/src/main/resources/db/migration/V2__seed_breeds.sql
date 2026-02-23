-- V2__seed_breeds.sql
-- Seed: przykladowe rasy psow (10 ras roznych grup i rzadkosci)
-- Pelna lista ~200 ras zostanie dodana w kolejnym kroku (V3 lub rozszerzenie V2)

INSERT INTO breed (name, name_pl, "group", size_category, description, rarity_score, is_active) VALUES
('Golden Retriever',     'Golden Retriever',       'Sporting',    'large',  'Przyjazny i energiczny pies myśliwski, znany z lojalności i inteligencji.',       1, true),
('Labrador Retriever',   'Labrador Retriever',     'Sporting',    'large',  'Najbardziej popularny pies w USA. Przyjazny, aktywny, doskonały do rodziny.',     1, true),
('German Shepherd',      'Owczarek Niemiecki',      'Herding',     'large',  'Niezwykle inteligentny i wszechstronny. Czesto uzywany w sluzbach mundurowych.',  2, true),
('French Bulldog',       'Buldog Francuski',        'Non-Sporting','small',  'Zwarta budowa, duze uszy jak u nietoperza. Idealny do mieszkania w miescie.',    2, true),
('Siberian Husky',       'Husky Syberyjski',        'Working',     'medium', 'Pies robociaLub z Syberii, wytrzymaly, towarzyski, wymaga duzej aktywnosci.',    2, true),
('Dachshund',            'Jamnik',                  'Hound',       'small',  'Dlugie cialo i krotkie nogi. Odwazny, dociekliwy i bardzo lojalny mysliwy.',     2, true),
('Border Collie',        'Border Collie',           'Herding',     'medium', 'Uznawany za najinteligentniejsza rase na swiecie. Wymaga duzej stymulacji.',     3, true),
('Shiba Inu',            'Shiba Inu',               'Non-Sporting','small',  'Japońska rasa mysliwska. Niezalezny, czysty, z silnym instynktem lowieckim.',  3, true),
('Afghan Hound',         'Chart Afganski',          'Hound',       'large',  'Jedna z najstarszych ras. Elegancki, niezalezny, z dluga elaza szata.',        4, true),
('Azawakh',              'Azawakh',                 'Hound',       'large',  'Rzadki zachodnioafrykanski chart. Niezwykla smukla sylwetka i wielka predkosc.', 5, true);
