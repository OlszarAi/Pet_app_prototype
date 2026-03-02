-- V3__seed_breeds_extended.sql
-- Rozszerzony seed ras psow — pokrycie głownych grup AKC/FCI
-- Uzupelnienie V2 (10 ras). Nie modyfikujemy V2.
-- Rarity score: 1=pospolita, 2=czesta, 3=rzadka, 4=bardzo rzadka, 5=unikat

INSERT INTO breed (name, name_pl, "group", size_category, description, rarity_score, is_active) VALUES

-- =====================
-- SPORTING GROUP
-- =====================
('Cocker Spaniel',             'Coker Spaniel',               'Sporting', 'medium', 'Wesoly i czuly pies mysliwski o jedwabistej sierci. Doskonaly kompan rodzinny.',         1, true),
('English Springer Spaniel',   'Springer Spaniel Angielski',  'Sporting', 'medium', 'Energiczny spaniel myśliwski. Zwinny, wytrzymaly i inteligentny.',                       2, true),
('Brittany',                   'Breton',                      'Sporting', 'medium', 'Zwinny francuski spaniel wskazujacy. Aktywny i latwy w szkoleniu.',                       2, true),
('Vizsla',                     'Wyżeł Wegierski',             'Sporting', 'medium', 'Elegancki wyzly wegierski o zloto-rudej sierci. Czuły i aktywny.',                        2, true),
('Weimaraner',                 'Wyżeł Weimarski',             'Sporting', 'large',  'Charakterystyczny szary pies mysliwski z jasnymi oczami. Atletyczny i lojalny.',          2, true),
('Irish Setter',               'Seter Irlandzki',             'Sporting', 'large',  'Przepiekny mahoniowy seter o energicznym temperamencie. Bardzo towarzyski.',              2, true),
('German Shorthaired Pointer', 'Wyżeł Niemecki Krótkowłosy',  'Sporting', 'large',  'Wszechstronny pies myśliwski. Wybitna wech i wytrzymałosc terenowa.',                     2, true),
('Nova Scotia Duck Tolling Retriever', 'Retriever z Nowej Szkocji', 'Sporting', 'medium', 'Najmniejszy z retrieverow. Unikalny sposob wabienia kaczek do myśliwego.',          3, true),
('Flat-Coated Retriever',      'Retriever Plasko Owłosiony',  'Sporting', 'large',  'Optymistyczny czarny lub watrobiasty retriever. Zachowuje szczeniacki charakter.',         3, true),

-- =====================
-- HOUND GROUP
-- =====================
('Beagle',                     'Beagle',                      'Hound',    'small',  'Kompaktowy, wesoły ogar. Jeden z najpopularniejszych psów na swiecie.',                   1, true),
('Basset Hound',               'Basset Hound',                'Hound',    'medium', 'Ogar o dlugich uszach i smutnych oczach. Doskonały nos, uparty i spokojny.',              1, true),
('Bloodhound',                 'Ogar Sw. Huberta',            'Hound',    'large',  'Najlepszy tropiący nos na świecie — odcisk zapachu sprzed 300h. Lagodny koloss.',         2, true),
('Greyhound',                  'Chart Angielski',             'Hound',    'large',  'Najszybszy pies swiata (72 km/h). Delikatny i spokojny mimo atletycznej sylwetki.',       2, true),
('Whippet',                    'Whippet',                     'Hound',    'medium', 'Miniaturowy chart. Szybki jak wiatr, w domu niezwykle lagodny i czuly.',                  2, true),
('Rhodesian Ridgeback',        'Ridgeback Rodezyjski',        'Hound',    'large',  'Afrykanski lew-pies ze specyficznym grzbietowym pasem. Silny i niezalezny.',              2, true),
('Saluki',                     'Chart Perski',                'Hound',    'large',  'Jedna z najstarszych ras swiata. Smukly, szybki, o niezwyklej elegancji.',                3, true),
('Norwegian Elkhound',         'Elkhound Norweski',           'Hound',    'medium', 'Skandynawski pies lowiecki zdolny polowac na losa. Bardzo wytrzymaly.',                   3, true),
('Ibizan Hound',               'Chart Ibizanski',             'Hound',    'large',  'Starożytny chart ze śródziemnomorskich wysp. Akrobatyczny skoczek.',                      4, true),
('Pharaoh Hound',              'Chart Faraona',               'Hound',    'medium', 'Narodowy pies Malty. Jedyna rasa, ktora sie czerwieni z podniecenia.',                    4, true),

-- =====================
-- WORKING GROUP
-- =====================
('Rottweiler',                 'Rottweiler',                  'Working',  'large',  'Silny pies strazniczy i pasterski z Niemiec. Pewny siebie i lojalny.',                    1, true),
('Doberman Pinscher',          'Doberman',                    'Working',  'large',  'Smukly i atletyczny stróznik. Inteligentny, szybki, doskonaly w obronie.',                2, true),
('Boxer',                      'Bokser',                      'Working',  'large',  'Energiczny i zabawny pies z charakterystycznym pyskem. Uwielbia dzieci.',                 1, true),
('Great Dane',                 'Dog Niemiecki',               'Working',  'large',  'Jeden z najwyzszych psow swiata. Lagodny koloss o przyjaznym usposobieniu.',              2, true),
('Saint Bernard',              'Bernardyn',                   'Working',  'large',  'Legendarny ratownik alpejski. Spokojny, cierpliwy i przytulny.',                          2, true),
('Bernese Mountain Dog',       'Bernenski Pies Pasterski',   'Working',  'large',  'Trikolorowy szwajcarski pies gorski. Lagodny, cierpliwy, uwielbia śnieg.',                2, true),
('Alaskan Malamute',           'Malamut Alaskański',          'Working',  'large',  'Potezny pies zaprzegowy Alaski. Wytrzymaly, niezalezny i towarzyski.',                    2, true),
('Samoyed',                    'Samojed',                     'Working',  'large',  'Bialy pies z Syberii z charakterystycznym usmiechem. Ciepły i towarzyski.',               2, true),
('Akita',                      'Akita',                       'Working',  'large',  'Japoński pies ciezki z legendy o wiernosci Hachiko. Dostojny i niezalezny.',              3, true),
('Leonberger',                 'Leonberger',                  'Working',  'large',  'Majestatyczny lew-pies z Niemiec. Lagodny olbrzym o cienistej sierci.',                   3, true),
('Anatolian Shepherd Dog',     'Owczarek Anatolijski',        'Working',  'large',  'Turecki stróz stada trzody. Niezwykle silny, niezalezny i odwazny.',                      3, true),

-- =====================
-- HERDING GROUP
-- =====================
('Australian Shepherd',        'Owczarek Australijski',       'Herding',  'medium', 'Energiczny pasterz z wyrazistym marmurowym umaszczeniem. Lubi zadania.',                 1, true),
('Belgian Malinois',           'Malinois Belgijski',          'Herding',  'medium', 'Pies policyjny i wojskowy par excellence. Intensywny, szybki, pracowitny.',              2, true),
('Rough Collie',               'Collie Zorstkowlosa',         'Herding',  'large',  'Lassie. Elegancki i inteligentny pasterz szkocki.',                                       1, true),
('Shetland Sheepdog',          'Szpic Miniaturowy Sheltie',   'Herding',  'small',  'Miniaturowy collie. Czujny, inteligentny i wierny jak cien.',                             2, true),
('Pembroke Welsh Corgi',       'Corgi Walijski Pembroke',     'Herding',  'small',  'Krolewski ulubieniec Elzbieto II. Wesoly, zwinny, z krotkimi nozkami.',                   1, true),
('Cardigan Welsh Corgi',       'Corgi Walijski Cardigan',     'Herding',  'small',  'Starszy z obu corgi. Roznia sie dlugo ogonem i szerszymi uszami. Lojalny.',              3, true),
('Australian Cattle Dog',      'Heeler Australijski',         'Herding',  'medium', 'Wytrzymaly pies bydlecy z outbacku. Niestudzony, sprytny, nieustepliwy.',                 2, true),
('Old English Sheepdog',       'Bobtail',                     'Herding',  'large',  'Kudlaty owczarek angielski znany z reklam. Wesoly, hałasliwy, niezwykle futrzany.',       2, true),

-- =====================
-- TERRIER GROUP
-- =====================
('West Highland White Terrier','Westie',                      'Terrier',  'small',  'Bialy szkocki terier. Pelny witalności, pewny siebie i uroczy.',                          1, true),
('Jack Russell Terrier',       'Jack Russell Terier',         'Terrier',  'small',  'Mały, nieuleczalnie energiczny pies myśliwski. Specjalista od nor.',                      1, true),
('Scottish Terrier',           'Scotch Terier',               'Terrier',  'small',  'Mały czarny lub pszeniczny Szkot. Niezalezny, odwazny i dostojny.',                       2, true),
('Bull Terrier',               'Bull Terier',                 'Terrier',  'medium', 'Charakterystyczne jajowate cielsko i niepowtarzalna glowa. Wojowniczy lecz czuly.',       2, true),
('Airedale Terrier',           'Airedale Terier',             'Terrier',  'large',  'Krol terierow — największy z nich. Inteligentny i wszechstronny.',                        2, true),
('Staffordshire Bull Terrier', 'Stafordshire Bull Terier',    'Terrier',  'medium', 'Zwarty i silny o szerokim usmieechu. Wbrew opinii — niezwykle lagodny z ludźmi.',         2, true),
('Yorkshire Terrier',          'Terier Yorkshirski',          'Terrier',  'small',  'Tiny but fierce. Jedwabista siers i wielka osobowosc w malutkim ciele.',                   1, true),

-- =====================
-- TOY GROUP
-- =====================
('Chihuahua',                  'Chihuahua',                   'Toy',      'small',  'Najmniejsza rasa swiata. Odwazny do granic, wierny jednej osobie.',                       1, true),
('Maltese',                    'Maltanczyk',                  'Toy',      'small',  'Śródziemnomorski arystokrata. Bialy jak snieg, lagodny i kulturalny.',                    2, true),
('Pomeranian',                 'Szpic Miniaturowy',           'Toy',      'small',  'Puszysty i energiczny szpic. Cechuje go zuchwalosc znacznie wieksza niz jego rozmiar.',   1, true),
('Cavalier King Charles Spaniel','Cavalier King Charles',     'Toy',      'small',  'Elegancki i niezmiernie uczuciowy. Wiecznie wesoly i gotowy do przytulania.',             1, true),
('Pug',                        'Mops',                        'Toy',      'small',  'Mnisi towarzysz o pomarszczonym pysku. Smieszny, spokojny i towarzyski.',                  1, true),
('Shih Tzu',                   'Shih Tzu',                    'Toy',      'small',  'Lew w miniaturze — tybetański pies swiatynny. Dumny, czuly i puszysty.',                   1, true),

-- =====================
-- NON-SPORTING GROUP
-- =====================
('Standard Poodle',            'Pudel Duzy',                  'Non-Sporting', 'large',  'Najbardziej inteligentna rasa po Border Collie. Atletyczny i wielostosowalny.',        1, true),
('Miniature Poodle',           'Pudel Miniaturowy',           'Non-Sporting', 'small',  'Mala wersja pudla — tyle samo inteligencji, mniej miejsca.',                           1, true),
('Dalmatian',                  'Dalmatynczyk',                'Non-Sporting', 'large',  'Biały z czarnymi cętkami. Energiczny, wytrzymaly — historyczny pies karety.',          2, true),
('Chow Chow',                  'Chow Chow',                   'Non-Sporting', 'large',  'Krotki jak niedzwiedz, z czarnym jezykiem. Niezalezny i dostojestwowy.',              2, true),
('Boston Terrier',             'Terier Bostonski',            'Non-Sporting', 'small',  'Amerykanski gentleman w smokingu. Zabawny, delikatny i zalezny od czlowieka.',         1, true),
('Chinese Shar Pei',           'Shar Pei',                    'Non-Sporting', 'medium', 'Arsztokrata zmarszczek — zmarszczona skora i niebiesko-czarny jezyk.',                3, true),
('Keeshond',                   'Szpic Wilczy',                'Non-Sporting', 'medium', 'Holenderski szpic o srebrno-czarnej sierci i spektakularych okularach.',               3, true),
('Lhasa Apso',                 'Lhasa Apso',                  'Non-Sporting', 'small',  'Tybetanski stroz klasztoru. Niezalezny, czujny, z dluga luksusowa siercia.',           3, true),

-- =====================
-- RASY RZADKIE I UNIKATOWE (rarity 4-5)
-- =====================
('Basenji',                    'Basenji',                     'Hound',    'small',  'Jedyny pies ktory nie szczeka — tylko "jodluje". Pradawna rasa z Afryki.',                4, true),
('Thai Ridgeback',             'Ridgeback Tajski',            'Hound',    'medium', 'Stara rasa z Tajlandii z grzbietowym rowem fu. Bardzo rzadki poza Azja.',                4, true),
('Xoloitzcuintli',             'Ksolo',                       'Non-Sporting', 'medium', 'Bezwlosa rasa aztecka. Jedna z najstarszych i najrzadszych ras Ameryki.',             5, true),
('Cirneco dell Etna',          'Cirneco z Etny',              'Hound',    'small',  'Starożytny sycylijski chart myśliwski. Bardzo rzadki poza Wlochami.',                     5, true),
('Kai Ken',                    'Kai Ken',                     'Non-Sporting', 'medium', 'Japońska rasa gorska o unikalnym tygrysim umaszczeniu. Relikt prehistorii.',          5, true);
