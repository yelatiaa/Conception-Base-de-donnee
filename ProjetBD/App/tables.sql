CREATE TABLE Utilisateur(
    email VARCHAR(200) PRIMARY KEY,
    nomUtilisateur VARCHAR(200),
    prenom VARCHAR(200),
    adresse VARCHAR(200)
);


CREATE TABLE Categorie(
                          nomCat VARCHAR(200) PRIMARY KEY,
                          descCat VARCHAR(200)
);

CREATE TABLE Salle(
                      idSalle INT PRIMARY KEY,
                      nomCat VARCHAR(200),
                      CONSTRAINT fk_salleNomCAT FOREIGN KEY (nomCAT) REFERENCES Categorie (nomCat)
);

CREATE TABLE Produit(
                        idProduit INT PRIMARY KEY,
                        nomProduit VARCHAR(200),
                        prixRevientUnite INT,
                        stock INT,
                        nomCat VARCHAR(200),
                        CONSTRAINT fk_produitNomCat FOREIGN KEY (nomCAT) REFERENCES Categorie (nomCat)
);

CREATE TABLE Vente(
    idVente INT PRIMARY KEY,

    idProduit INT,
    CONSTRAINT fk_idProduit FOREIGN KEY (idProduit) REFERENCES PRODUIT (idProduit),

    idSalle INT,
    CONSTRAINT fk_idSalle FOREIGN KEY (idSalle) REFERENCES SALLE (idSalle),
    
    revocable NUMBER(1),
    offreUnique NUMBER(1),
    limite NUMBER(1),
    descendante NUMBER(1),

    prixDepart INT,
    dateVente DATE NOT NULL
);

CREATE TABLE VenteLimite(
    idVente INT PRIMARY KEY,
    CONSTRAINT fk_idVentelim FOREIGN KEY (idVente) REFERENCES Vente (idVente),
    dateHeureFin DATE NOT NULL
);


CREATE TABLE VenteDescendante(
    idVente INT PRIMARY KEY,
    CONSTRAINT fk_idVentedesc FOREIGN KEY (idVente) REFERENCES Vente (idVente),
    diminutionRate INT NOT NULL,
    quantiteRestante INT NOT NULL
);



CREATE TABLE Caracteristique(
    nomCar VARCHAR(200) PRIMARY KEY
);




CREATE TABLE CaracteristiqueProduit(
    idProduit INT,
    CONSTRAINT fk_caracteristiqueIdProduit FOREIGN KEY (idProduit) REFERENCES Produit (idProduit),
    nomCar VARCHAR(200),
    CONSTRAINT fk_caracteristiqueNomCar FOREIGN KEY (nomCar) REFERENCES Caracteristique (nomCar),
    valCar INT
);

CREATE TABLE Offre(
    email VARCHAR(200),
    CONSTRAINT fk_OffreUtilisateur FOREIGN KEY (email) REFERENCES Utilisateur (email),
    idVente INT,
    CONSTRAINT fk_OffreVente FOREIGN KEY (idVente) REFERENCES Vente (idVente),
    quantite INT,
    dateHeureOffre DATE NOT NULL,
    prixOffre INT CONSTRAINT prixOffrePosituf CHECK (prixOffre > 0)
);

