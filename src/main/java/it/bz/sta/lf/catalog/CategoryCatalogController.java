package it.bz.sta.lf.catalog;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/catalog")
public class CategoryCatalogController {

    public record SubCategory(String code, Map<String, String> label) {}
    public record Category(String code, Map<String, String> label, String iconKey, List<SubCategory> subcategories) {}

    @GetMapping("/categories")
    public List<Category> categories() {
        return List.of(
                category_ID_DOCS(),
                category_VEHICLES(),
                category_CLOTHING(),
                category_WALLETS_MONEY(),
                category_GLASSES_OPTICAL(),
                category_ELECTRONICS(),
                category_BIKES_SCOOTERS(),
                category_HOUSEHOLD_TOOLS(),
                category_BAGS_CASES(),
                category_MEDICAL(),
                category_MUSICAL(),
                category_FOOD_TOBACCO(),
                category_UMBRELLAS(),
                category_KEYS(),
                category_JEWELRY_WATCHES(),
                category_STATIONERY_BOOKS_PHOTOS(),
                category_TOYS(),
                category_SPORT_LEISURE(),
                category_ANIMALS(),
                category_MISC()
        );
    }

    // ---------------- MAIN CATEGORIES ----------------

    private static Category category_ID_DOCS() {
        return new Category(
                "ID_DOCS",
                Map.of("it","Documenti d’identità, documenti, tessere", "de","Ausweise, Dokumente, Plastikkarten", "en","ID, documents, plastic cards"),
                "id-card",
                List.of(
                        sub("ANNUAL_PASS_TICKET",
                                "Tessera annuale, biglietto",
                                "Jahreskarte, Fahrkarte",
                                "Annual pass, travel ticket"),
                        sub("DOCUMENT_GENERAL",
                                "Documento (certificato di cittadinanza, certificato di nascita, ecc.)",
                                "Dokument (Staatsbürgerschaftsnachweis, Geburtsurkunde, …)",
                                "Document (proof of citizenship, birth certificate, etc.)"),
                        sub("DRIVING_LICENSE",
                                "Patente di Guida",
                                "Führerschein",
                                "Driving license"),
                        sub("EMPLOYEE_CARD",
                                "Tessera dipendente, distintivo aziendale",
                                "Mitarbeiterkarte, Firmenausweis",
                                "Employee card, company ID card"),
                        sub("HEALTH_INSURANCE_CARD",
                                "Tessera di Assicurazione malattia",
                                "Krankenversicherungskarte",
                                "Health insurance card"),
                        sub("IDENTITY_CARD",
                                "Carta d’identità (documento personale, distintivo scolastico, tessera universitaria, ecc.)",
                                "Identitätskarte (Personalausweis, Schülerausweis, Studentenausweis, …)",
                                "Identity card (ID card, student card, etc.)"),
                        sub("OTHER_ID_DOCS",
                                "Altri documenti d’identità, documenti, tessere di plastica",
                                "Sonstige Ausweise, Dokumente, Plastikkarten",
                                "Other ID, documents, plastic cards"),
                        sub("PASSPORT",
                                "Passaporto",
                                "Pass, Reisepass",
                                "Passport"),
                        sub("PAYMENT_CARD",
                                "Carta di Pagamento (carta di credito, girocard, carta di debito, tessera bancomat, ecc.)",
                                "Zahlungskarte (Kreditkarte, EC-Karte, Bankomatkarte, …)",
                                "Payment card (credit/debit/ATM card, etc.)"),
                        sub("RESIDENCE_PERMIT",
                                "Titolo di soggiorno",
                                "Aufenthaltstitel",
                                "Residence permit"),
                        sub("VACCINATION_CERT",
                                "Certificato di Vaccinazione",
                                "Impfpass",
                                "Vaccination passport"),
                        sub("VEHICLE_DOCUMENT",
                                "Documento veicolo (carta di circolazione, certificato di proprietà, ecc.)",
                                "Fahrzeugdokument (Zulassungsschein, Fahrzeugbrief, …)",
                                "Vehicle document (registration, ownership, etc.)")
                )
        );
    }

    private static Category category_VEHICLES() {
        return new Category(
                "VEHICLES",
                Map.of("it","Auto, ciclomotori, natanti, rimorchi", "de","Auto, Motorräder, Boote, Anhänger", "en","Vehicles, motorbikes, boats, trailers"),
                "car",
                List.of(
                        sub("BOAT", "Natante", "Boot", "Boat"),
                        sub("CAR", "Auto (vettura)", "Auto (PKW)", "Car (passenger)"),
                        sub("LICENSE_PLATE", "Numero d’immatricolazione del veicolo, numero di targa", "KFZ-Kennzeichen, Nummernschild", "License plate, registration plate"),
                        sub("MOTORCYCLE", "Ciclomotore", "Motorrad", "Motorcycle"),
                        sub("OTHER_VEHICLE", "Altro veicolo", "Sonstiges Fahrzeug", "Other vehicle"),
                        sub("TRAILER", "Rimorchio", "Anhänger", "Trailer"),
                        sub("VEHICLE_ACCESSORY", "Accessori veicolo", "Fahrzeug-Zubehör", "Vehicle accessory")
                )
        );
    }

    private static Category category_CLOTHING() {
        return new Category(
                "CLOTHING",
                Map.of("it","Abbigliamento, Scarpe", "de","Bekleidung, Schuhe", "en","Clothing, footwear"),
                "shirt",
                List.of(
                        sub("BABY_CLOTHING","Abbigliamento bebè","Babykleidung","Baby clothing"),
                        sub("SWIMWEAR","Abbigliamento mare-piscina","Badebekleidung","Swimwear"),
                        sub("OTHER_CLOTHING","Altro abbigliamento","Sonstige Bekleidung","Other clothing"),
                        sub("SHIRT_BLOUSE","Camicia uomo, camicetta","Hemd, Bluse","Shirt, blouse"),
                        sub("COAT_JACKET","Cappotto, giacchetta, blazer, giacca","Mantel, Jacke, Blazer, Sakko","Coat, jacket, blazer"),
                        sub("BELT","Cintura","Gürtel","Belt"),
                        sub("HEADGEAR","Copricapo (berretto, cuffia, cappello, ecc.)","Kopfbedeckung (Mütze, Haube, Hut, …)","Headgear (cap, hat, etc.)"),
                        sub("SCARF","Fazzoletto da collo, sciarpa","Halstuch, Schal","Scarf, shawl"),
                        sub("SKIRT","Gonna","Rock","Skirt"),
                        sub("GLOVES","Guanti, muffole","Handschuhe, Fäustlinge","Gloves, mittens"),
                        sub("VEST","Panciotto, gilet","Weste, Gilet","Vest, gilet"),
                        sub("TROUSERS","Pantaloni","Hose","Trousers"),
                        sub("SWEATER","Pullover, felpa","Pullover, Sweatshirt","Sweater, sweatshirt"),
                        sub("FOOTWEAR","Scarpe","Schuhe","Footwear"),
                        sub("TSHIRT_POLO","T-Shirt, polo","T-Shirt, Polo","T-shirt, polo"),
                        sub("DRESS","Vestito","Kleid","Dress")
                )
        );
    }

    private static Category category_WALLETS_MONEY() {
        return new Category(
                "WALLETS_MONEY",
                Map.of("it","Portafoglio, soldi, titoli", "de","Brieftasche, Geld, Wertpapiere", "en","Wallets, money, securities"),
                "wallet",
                List.of(
                        sub("OTHER_SECURITIES","Altri titoli","Sonstige Wertpapiere","Other securities"),
                        sub("PAWN_TICKET","Atto di pegno","Pfandschein","Pawn ticket"),
                        sub("EVENT_TICKET_VOUCHER","Biglietto di manifestazione, concerto, buono","Veranstaltungsticket, Konzertkarte, Gutschein","Event ticket, concert ticket, voucher"),
                        sub("WALLET_PURSE","Borsellino, portafoglio, portamonete","Geldbörse, Brieftasche, Portemonnaie","Wallet, purse"),
                        sub("CASH_CURRENCY","Denaro contante, valuta","Geld, Valuten","Cash, foreign currency"),
                        sub("SAVINGS_BOOK","Libretto dei risparmi","Sparbuch","Savings book"),
                        sub("COMMEMORATIVE_COIN","Moneta speciale","Sondermünze","Commemorative coin"),
                        sub("CARDHOLDER","Portatessere","Kartenetui","Cardholder")
                )
        );
    }

    private static Category category_GLASSES_OPTICAL() {
        return new Category(
                "GLASSES_OPTICAL",
                Map.of("it","Occhiali, lenti a contatto, dispositivi ottici", "de","Brillen, Kontaktlinsen, optische Geräte", "en","Glasses, contact lenses, optical equipment"),
                "glasses",
                List.of(
                        sub("OPTICAL_ACCESSORIES","Accessori occhiali, lenti a contatto, dispositivi ottici","Zubehör Brillen, Kontaktlinsen, optische Geräte","Glasses accessories, contact lenses, optical equipment"),
                        sub("GLASSES_CASE","Astuccio per occhiali","Brillenetui","Glasses case"),
                        sub("BINOCULARS","Cannocchiale, binocolo","Feldstecher, Fernglas","Binoculars, spyglass"),
                        sub("SUNGLASSES","Occhiali da sole","Sonnenbrille","Sunglasses"),
                        sub("CHILDRENS_GLASSES","Occhiali per bambini","Kinderbrille","Children’s glasses"),
                        sub("OPTICAL_GLASSES","Occhiali professionali / lettura / vista","Optische Brille, Korrekturbrille","Optical glasses, reading glasses, corrective glasses")
                )
        );
    }

    private static Category category_ELECTRONICS() {
        return new Category(
                "ELECTRONICS",
                Map.of("it","Elettronica, foto, cellulari", "de","Elektronik, Kameras, Handys", "en","Electronics, cameras, cellphones"),
                "phone",
                List.of(
                        sub("ELECTRONIC_ACCESSORIES","Accessori elettronici","Elektronikzubehör","Electronic accessories"),
                        sub("OTHER_ELECTRONICS","Altra elettronica","Sonstige Elektronik","Other electronics"),
                        sub("PHONE_SMARTPHONE","Cellulare, smartphone","Handy, Smartphone","Cellphone, smartphone"),
                        sub("LAPTOP_NOTEBOOK","Computer portatile, Notebook","Laptop, Notebook","Laptop, notebook"),
                        sub("HEADPHONES","Cuffie auricolari","Kopfhörer","Headphones"),
                        sub("SPEAKERS","Casse, altoparlanti","Boxen, Lautsprecher","Amplifiers, speaker"),
                        sub("DRONE","Drone","Drohne","Drone"),
                        sub("CHARGING_CASE","Custodia di ricarica (senza cuffie)","Ladecase (ohne Kopfhörer)","Charging case (without headphones)"),
                        sub("CALCULATOR","Calcolatrice tascabile","Taschenrechner","Calculator"),
                        sub("ELECTRONIC_TOY","Giocattolo elettronico (Gameboy, ecc.)","Elektronisches Spielzeug","Electronic toy (Gameboy, etc.)"),
                        sub("MP3_PLAYER","Lettore MP3, iPod, Discman, Walkman","MP3-Player, iPod, Discman, Walkman","MP3 player, iPod, Discman, Walkman"),
                        sub("CAMERA","Macchina fotografica, videocamera e accessori","Fotoapparat, Videokamera und Zubehör","Camera, video camera and accessories"),
                        sub("NAVIGATION_GPS","Navigatore GPS portatile","Navigationsgerät, GPS-Handgerät","Navigation device, handheld GPS device"),
                        sub("PC","PC, Computer","PC, Computer","PC, computer"),
                        sub("POWERBANK_CHARGER","Power bank, Caricabatteria","Powerbank, Ladegerät","Power bank, charger"),
                        sub("DATA_MEDIA","Supporto dati (USB, CD, DVD, disco rigido, ecc.)","Datenträger (USB, CD, DVD, Festplatte, …)","Data media (USB stick, CD, DVD, hard drive etc.)"),
                        sub("TABLET_EBOOK","Tablet, lettore E-book","Tablet, E-Book-Reader","Tablet, e-book reader"),
                        sub("PROJECTOR_TV_MONITOR","Videoproiettore / TV / Schermo","Beamer / TV / Bildschirm","Projector, TV, monitor")
                )
        );
    }

    private static Category category_BIKES_SCOOTERS() {
        return new Category(
                "BIKES_SCOOTERS",
                Map.of("it","Biciclette, scooter, passeggini", "de","Fahrräder, Roller, Kinderwägen", "en","Bicycles, scooters, pushchairs"),
                "bike",
                List.of(
                        sub("BIKE_ACCESSORIES","Accessori per biciclette","Fahrrad-Zubehör","Bicycle accessories"),
                        sub("PUSHCHAIR_ACCESSORIES","Accessori per carrozzina","Kinderwagenzubehör","Pram/Pushchair accessories"),
                        sub("OTHER_BICYCLE","Altra bicicletta","Sonstiges Fahrrad","Other bicycle"),
                        sub("BMX","Bici BMX","BMX-Rad","BMX bike"),
                        sub("RACING_BIKE","Bici da corsa","Rennrad","Racing bike"),
                        sub("CROSS_BIKE","Bici da cross","Crossbike","Cross bike"),
                        sub("WOMENS_BIKE","Bici da donna","Damenrad","Ladies’ bike"),
                        sub("FITNESS_BIKE","Bici da fitness","Fitness Bike","Fitness bike"),
                        sub("TREKKING_BIKE","Bici da trekking","Trekking-Rad","Trekking bike"),
                        sub("MENS_BIKE","Bici da uomo","Herrenrad","Men’s bike"),
                        sub("YOUTH_BIKE","Bici per giovani","Jugendrad","Youth bike"),
                        sub("SINGLE_SPEED","Bici singlespeed","Singlespeed-Rad","Singlespeed bike"),
                        sub("UNISEX_BIKE","Bici unisex","Unisex-Rad","Unisex bike"),
                        sub("E_BIKE","Bicicletta elettrica","E-Bike","E-bike"),
                        sub("CHILD_BIKE","Bicicletta per bambini, triciclo","Kinderfahrrad, Dreirad","Children’s bicycle, tricycle"),
                        sub("FOLDING_BIKE","Bicicletta pieghevole","Klapprad","Folding bike"),
                        sub("PUSHCHAIR","Carrozzina","Kinderwagen","Pram/Pushchair"),
                        sub("CITYBIKE","City bike, bicicletta olandese","Citybike, Hollandrad","Citybike, roadster"),
                        sub("SCOOTER","Monopattino / scooter a spinta","Tretroller / Scooter","Scooter"),
                        sub("MOUNTAIN_BIKE","Mountain bike","Mountainbike","Mountain bike"),
                        sub("E_SCOOTER","Scooter elettrico","E-Scooter","E-scooter")
                )
        );
    }

    private static Category category_HOUSEHOLD_TOOLS() {
        return new Category(
                "HOUSEHOLD_TOOLS",
                Map.of("it","Prodotti per la casa, attrezzi", "de","Haushalt, Werkzeuge", "en","Household, tools"),
                "tools",
                List.of(
                        sub("OTHER_TOOLS","Altro attrezzo domestico, utensile","Sonstige Haushaltsgeräte, Werkzeug","Other household appliances, tool"),
                        sub("HOUSEHOLD_APPLIANCE","Attrezzo domestico","Haushaltgeräte","Household appliances"),
                        sub("KITCHEN_KNIFE_SCISSORS","Coltello da cucina, forbici","Küchenmesser, Schere","Kitchen knife, scissors"),
                        sub("POCKET_KNIFE_MULTI_TOOL","Coltello tascabile, coltello multiuso","Taschenmesser, Multi-Tool","Pocket knife, multi-tool"),
                        sub("FLASHLIGHT","Torcia elettrica","Taschenlampe","Flashlight"),
                        sub("POWER_TOOL","Utensile elettrico","Werkzeug elektrisch","Power tool"),
                        sub("MECHANIC_TOOL","Utensile meccanico","Werkzeug mechanisch","Mechanic tool")
                )
        );
    }

    private static Category category_BAGS_CASES() {
        return new Category(
                "BAGS_CASES",
                Map.of("it","Valige, zaini, borse", "de","Koffer, Rucksäcke, Taschen", "en","Cases, backpacks, bags"),
                "bag",
                List.of(
                        sub("OTHER_BAGS","Altre valige, zaini, borse","Sonstige Koffer, Rucksäcke, Taschen","Other cases, backpacks, bags"),
                        sub("HANDBAG","Borsa a mano, borsa a tracolla","Handtasche, Umhängetasche","Handbag, shoulder bag"),
                        sub("LAPTOP_CAMERA_BAG","Borsa porta notebook / foto / video","Laptoptasche, Fototasche, Videotasche","Laptop/camera/video bag"),
                        sub("DUFFEL_GYM_BAG","Borsone sport, borsone","Sporttasche, Reisetasche","Sports bag, travel bag"),
                        sub("BRIEFCASE","Cartella","Aktenkoffer, Schreibmappe","Briefcase / portfolio"),
                        sub("BOX_PACKAGE","Cartone, scatola, pacco","Karton, Schachtel, Paket","Box, package"),
                        sub("BASKET","Cestino, cestino per la spesa","Korb, Einkaufskorb","Basket, shopping basket"),
                        sub("GARMENT_BAG","Custodia porta abiti","Kleidersack","Garment bag"),
                        sub("FANNY_PACK","Marsupio, borsello alla vita","Bauchtasche, Lendentasche","Fanny pack, belt bag"),
                        sub("TOILETRY_BAG","Beauty / necessaire / pochette","Kulturbeutel, Necessaire","Toiletry bag, beauty case"),
                        sub("PLASTIC_PAPER_BAG","Sacchetto plastica/carta, borsa spesa","Plastiksack, Papiersack, Einkaufstasche","Plastic/paper/shopping bag"),
                        sub("TOOL_CASE","Valigetta fai da te","Handarbeitskoffer","Tool box / organizer"),
                        sub("DOCUMENT_CASE","Valigetta portadocumenti / pilotenkoffer","Aktenkoffer, Pilotenkoffer","Document case / pilot case"),
                        sub("SUITCASE_TROLLEY","Valigia, trolley","Reisekoffer, Trolley","Suitcase, trolley"),
                        sub("BACKPACK","Zaino","Rucksack","Backpack")
                )
        );
    }

    private static Category category_MEDICAL() {
        return new Category(
                "MEDICAL",
                Map.of("it","Dispositivi e ausili medicali, farmaci, cosmetici", "de","Medizinische Geräte, Hilfsmittel, Medikamente", "en","Medical devices and aids, medicines, cosmetics"),
                "medical",
                List.of(
                        sub("HEARING_AID","Apparecchio acustico","Hörgeräte","Hearing aid"),
                        sub("DENTAL_DEVICE","Apparecchio per denti","Zahnspange","Retainer / dental device"),
                        sub("WALKING_AID","Ausilio deambulazione (stampella, bastone, ecc.)","Gehhilfe (Krücke, Stock, …)","Walking aids"),
                        sub("COSMETICS","Cosmetici (profumo, ecc.)","Kosmetikartikel (Parfum, …)","Cosmetic products"),
                        sub("MEDICAL_DEVICE","Dispositivo e ausilio medico","Medizinisches Gerät und Hilfsmittel","Medical device and aid"),
                        sub("MEDICINES","Farmaci","Medikamente","Medicines"),
                        sub("XRAY","Lastra medica (radiografia, ecc.)","Medizinische Aufnahme (Röntgenbild, …)","Medical record (x-ray, etc.)"),
                        sub("PROSTHESIS","Protesi","Prothese","Prosthesis"),
                        sub("DENTURES","Protesi dentaria, dentiera","Zahnprothese, Gebiss","Dental prosthesis, dentures"),
                        sub("WHEELCHAIR","Sedie a rotelle","Rollstuhl","Wheelchair")
                )
        );
    }

    private static Category category_MUSICAL() {
        return new Category(
                "MUSICAL",
                Map.of("it","Strumenti musicali", "de","Musikinstrumente", "en","Musical instruments"),
                "music",
                List.of(
                        sub("MUSICAL_ACCESSORY","Accessori per strumenti musicali","Musikinstrumentenzubehör","Musical instrument accessory"),
                        sub("OTHER_MUSICAL","Altri strumenti musicali","Sonstige Musikinstrumente","Other musical instruments"),
                        sub("STRING_INSTRUMENT","Strumento a corda (chitarra, ecc.)","Saiteninstrument (Gitarre, …)","Stringed instrument"),
                        sub("WIND_INSTRUMENT","Strumenti a fiato (tromba, ecc.)","Blasinstrument (Trompete, …)","Wind instrument"),
                        sub("KEYBOARD_INSTRUMENT","Strumento a tasti (tastiera, ecc.)","Tasteninstrument (Keyboard, …)","Keyboard instrument"),
                        sub("BOWED_INSTRUMENT","Strumento ad arco (violino, ecc.)","Streichinstrument (Geige, …)","Bowed instrument")
                )
        );
    }

    private static Category category_FOOD_TOBACCO() {
        return new Category(
                "FOOD_TOBACCO",
                Map.of("it","Alimentari e generi voluttuari", "de","Nahrungs- und Genussmittel", "en","Food, drink or tobacco"),
                "food",
                List.of(
                        sub("SPIRITS","Alcolici","Spirituosen","Spirits"),
                        sub("OTHER_FOOD_TOBACCO","Altri alimentari e generi voluttuari","Sonstige Nahrungs- und Genussmittel","Other food, drink or tobacco"),
                        sub("SMOKING_ACCESSORIES","Articoli per fumatori e accessori","Raucherwaren und Zubehör","Smoking products and accessories"),
                        sub("BOTTLE_THERMOS","Borraccia / tazza ermetica / thermos","Trinkflasche, Trinkbecher, Thermoskanne","Drinking bottle, cup, thermos"),
                        sub("CANDY","Dolciumi","Süßigkeiten","Candy"),
                        sub("E_CIGARETTE","Sigaretta elettronica","E-Zigarette","E-cigarette")
                )
        );
    }

    private static Category category_UMBRELLAS() {
        return new Category(
                "UMBRELLAS",
                Map.of("it","Ombrelli", "de","Schirme, Regenschirme", "en","Umbrellas"),
                "umbrella",
                List.of(
                        sub("OTHER_UMBRELLA","Altro ombrello","Sonstiger Schirm","Other umbrella"),
                        sub("UMBRELLA","Ombrello","Stockschirm","Stick umbrella"),
                        sub("CHILD_UMBRELLA","Ombrello per bambini","Kinderschirm","Children’s umbrella"),
                        sub("POCKET_UMBRELLA","Ombrello pieghevole / tascabile","Knirps, Taschenschirm","Pocket umbrella")
                )
        );
    }

    private static Category category_KEYS() {
        return new Category(
                "KEYS",
                Map.of("it","Chiavi", "de","Schlüssel", "en","Keys"),
                "key",
                List.of(
                        sub("VEHICLE_KEY_SINGLE","Chiave del veicolo (singola)","Einzel-Fahrzeugschlüssel","Single vehicle key"),
                        sub("KEY_SINGLE","Chiave singola","Einzel-Schlüssel","Single key"),
                        sub("KEYCHAIN","Portachiavi","Schlüsselanhänger","Key ring"),
                        sub("KEY_BUNCH_WITH_VEHICLE","Mazzo con chiave veicolo","Schlüsselbund mit Fahrzeugschlüssel","Bunch with vehicle key"),
                        sub("KEY_BUNCH_NO_VEHICLE","Mazzo senza chiave veicolo","(Schlüsselbund ohne Fahrzeugschlüssel)","Bunch of keys without vehicle key"),
                        sub("OTHER_KEYS","Altre chiavi","Sonstige Schlüssel","Other keys"),
                        sub("GATE_GARAGE_REMOTE","Telecomando cancello/garage","Toröffner, Garagenöffner (Fernbedienung)","Gate/garage opener (remote)"),
                        sub("ACCESS_CARD_CHIP","Tessera accesso / chiave elettronica / chip","Zutrittskarte, Elektronischer Schlüssel, Chip","Access card, electronic key, chip")
                )
        );
    }

    private static Category category_JEWELRY_WATCHES() {
        return new Category(
                "JEWELRY_WATCHES",
                Map.of("it","Gioielli, orologi", "de","Schmuck, Uhren", "en","Jewelry, watches"),
                "watch",
                List.of(
                        sub("OTHER_JEWELRY","Altri gioielli","Sonstiger Schmuck","Other jewelry"),
                        sub("OTHER_WATCH","Altri orologi","Sonstige Uhr","Other watch"),
                        sub("RING","Anello","Ring","Ring"),
                        sub("BRACELET","Bracciale, cavigliera","Armband, Fußkette","Bracelet, anklet"),
                        sub("NECKLACE","Collana","Halskette","Necklace"),
                        sub("EARRING","Orecchino / piercing","Ohrring, Piercing","Earring, piercing"),
                        sub("WRISTWATCH","Orologio da polso","Armbanduhr","Watch"),
                        sub("CHILD_WATCH","Orologio per bambini","Kinderuhr","Children’s watch"),
                        sub("SMARTWATCH","Smart watch","Smartwatch","Smartwatch"),
                        sub("BROOCH_CHARMS","Spilla / ciondoli / gemelli","Brosche, Anhänger, Manschettenknöpfe","Brooch, charms, cufflinks")
                )
        );
    }

    private static Category category_STATIONERY_BOOKS_PHOTOS() {
        return new Category(
                "STATIONERY_BOOKS_PHOTOS",
                Map.of("it","Cancelleria, libri, foto", "de","Schreibwaren, Bücher, Fotos", "en","Stationery, books, photos"),
                "book",
                List.of(
                        sub("NOTEBOOK_DIARY","Agenda, calendario, quaderno","Agenda, Kalender, Notizbuch","Diary, calendar, notebook"),
                        sub("OTHER_STATIONERY","Altre cancellerie, libri, foto","Sonstige Schreibwaren, Bücher, Fotos","Other stationery, books, photos"),
                        sub("PENCIL_CASE","Astuccio per matite","Federmappe, Schuletui","Pencil case"),
                        sub("STATIONERY","Cancelleria","Schreibwaren","Stationery"),
                        sub("PORTFOLIO_DRAWING","Cartella portadisegni","Zeichenmappe","Portfolio"),
                        sub("PICTURE_FRAME","Dipinto, cornice","Gemälde, Bilderrahmen","Picture, picture frame"),
                        sub("BOOK","Libro","Buch","Book"),
                        sub("PHOTO_ALBUM","Fotografia, album, fotolibro","Bild, Fotoalbum, Fotobuch","Photo, photo album, photo book"),
                        sub("TRANSPORT_TUBE","Tubo per trasporto","Transport-Rolle","Storage tube")
                )
        );
    }

    private static Category category_TOYS() {
        return new Category(
                "TOYS",
                Map.of("it","Giocattolo", "de","Spielzeug", "en","Toy"),
                "toy",
                List.of(
                        sub("OTHER_TOY","Altro giocattolo","Sonstige Spielzeug","Other toy"),
                        sub("DOLL_PLUSH","Bambola / peluche","Puppe, Stofftier, Kuscheltier","Doll, stuffed animal"),
                        sub("BABY_TOY","Giocattolo per bebè / bambini","Babyspielzeug, Kleinkindspielzug","Baby / toddler toy"),
                        sub("CARD_GAME","Gioco con le carte","Kartenspiel","Card game"),
                        sub("BOARD_GAME","Gioco da tavolo","Brettspiel","Board game"),
                        sub("REMOTE_CONTROL_VEHICLE","Veicolo telecomandato","Ferngesteuertes Fahrzeug","Remote control car")
                )
        );
    }

    private static Category category_SPORT_LEISURE() {
        return new Category(
                "SPORT_LEISURE",
                Map.of("it","Sport e tempo libero", "de","Sport- und Freizeitartikel", "en","Sports and leisure items"),
                "sports",
                List.of(
                        sub("SPORTS_APPAREL","Abbigliamento sportivo","Sportbekleidung","Sports apparel"),
                        sub("OTHER_SPORTS","Altri articoli sport/tempo libero","Sonstige Sport- und Freizeitartikel","Other sports and leisure items"),
                        sub("CAMPING","Articolo da campeggio","Campingartikel","Camping items"),
                        sub("RACKET","Bastone/mazza/racchetta","Schläger","Racket"),
                        sub("POLES","Bastoni passeggio / sci","Walking-, Wander-, Ski-Stöcke","Walking/hiking/ski poles"),
                        sub("FITNESS_BAND","Bracciale fitness","Fitness Armband","Fitness wristband"),
                        sub("HELMET","Casco","Helm","Helmet"),
                        sub("GOGGLES","Occhiali sci/snowboard","Ski-, Snowboardbrille","Ski/snowboard goggles"),
                        sub("BALL","Palla","Ball","Ball"),
                        sub("ICE_SKATES","Pattini da ghiaccio","Schlittschuhe","Ice skates"),
                        sub("INLINE_SKATES","Pattini in linea / rotelle","Inlineskater, Rollerskates","Inline skates, roller skates"),
                        sub("SKI_BOOTS","Scarponi sci/snowboard","Ski-, Snowboardschuhe","Ski/snowboard boots"),
                        sub("SKIS_SNOWBOARD","Sci / snowboard","(Langlauf-)Ski, Snowboard","Skis, snowboard"),
                        sub("SKATEBOARD","Skateboard / longboard","Skateboard, Longboard, Hoverboard","Skateboard, longboard, hoverboard"),
                        sub("SLED","Slittino, bob","Schlitten, Bob","Sled, bobsled")
                )
        );
    }

    private static Category category_ANIMALS() {
        return new Category(
                "ANIMALS",
                Map.of("it","Animali, accessori per animali", "de","Tiere, Tierzubehör", "en","Animals, animal accessories"),
                "paw",
                List.of(
                        sub("ANIMAL_ACCESSORIES","Accessori per animali","Tierzubehör","Animal accessories"),
                        sub("OTHER_ANIMALS","Altri animali","Sonstige Tiere","Other animals"),
                        sub("LIVESTOCK","Animale da allevamento","Nutztier","Livestock"),
                        sub("DOG","Cane","Hund","Dog"),
                        sub("CAT","Gatto","Katze","Cat"),
                        sub("SMALL_ANIMAL","Piccolo animale","Kleintier","Small animal"),
                        sub("REPTILE","Rettile","Reptil","Reptile"),
                        sub("BIRD","Uccello","Vogel","Bird")
                )
        );
    }

    private static Category category_MISC() {
        return new Category(
                "MISC",
                Map.of("it","Varie", "de","Diverses", "en","Miscellaneous"),
                "dots",
                List.of(
                        sub("OTHER","Altro","Sonstiges","Other")
                )
        );
    }

    // ---------------- helper ----------------

    private static SubCategory sub(String code, String it, String de, String en) {
        return new SubCategory(code, Map.of("it", it, "de", de, "en", en));
    }
}
