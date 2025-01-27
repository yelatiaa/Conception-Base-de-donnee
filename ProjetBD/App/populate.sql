-- Insertion dans la table Categorie (une seule catégorie)
INSERT INTO Categorie (nomCat, descCat)
VALUES ('Electronique', 'Tous les produits électroniques');


-- Insertion d'une salle
-- INSERT INTO Salle (idSalle, nomCat)
-- VALUES (0, 'Electronique');

-- Insertion dans la table Utilisateur (3 utilisateurs fictifs)
INSERT INTO Utilisateur (email, nomUtilisateur, prenom, adresse)
VALUES ('john.doe@example.com', 'john_doe', 'John', '123 Rue Exemple, Paris');

INSERT INTO Utilisateur (email, nomUtilisateur, prenom, adresse)
VALUES ('jane.smith@example.com', 'jane_smith', 'Jane', '456 Avenue Exemple, Lyon');

INSERT INTO Utilisateur (email, nomUtilisateur, prenom, adresse)
VALUES ('bob.jones@example.com', 'bob_jones', 'Bob', '789 Boulevard Exemple, Marseille');

-- Insertion dans la table Produit (10 produits fictifs dans la catégorie Electronique)
INSERT INTO Produit (idProduit, nomProduit, prixRevientUnite, stock, nomCat)
VALUES (1, 'Smartphone', 300, 50, 'Electronique');

INSERT INTO Produit (idProduit, nomProduit, prixRevientUnite, stock, nomCat)
VALUES (2, 'Laptop', 1000, 30, 'Electronique');

INSERT INTO Produit (idProduit, nomProduit, prixRevientUnite, stock, nomCat)
VALUES (3, 'Télévision', 600, 20, 'Electronique');

INSERT INTO Produit (idProduit, nomProduit, prixRevientUnite, stock, nomCat)
VALUES (4, 'Montre connectée', 150, 40, 'Electronique');

INSERT INTO Produit (idProduit, nomProduit, prixRevientUnite, stock, nomCat)
VALUES (5, 'Casque audio', 80, 70, 'Electronique');

INSERT INTO Produit (idProduit, nomProduit, prixRevientUnite, stock, nomCat)
VALUES (6, 'Appareil photo', 500, 15, 'Electronique');

INSERT INTO Produit (idProduit, nomProduit, prixRevientUnite, stock, nomCat)
VALUES (7, 'Tablette', 350, 50, 'Electronique');

INSERT INTO Produit (idProduit, nomProduit, prixRevientUnite, stock, nomCat)
VALUES (8, 'Ordinateur de bureau', 1200, 10, 'Electronique');

INSERT INTO Produit (idProduit, nomProduit, prixRevientUnite, stock, nomCat)
VALUES (9, 'Clé USB', 20, 200, 'Electronique');

INSERT INTO Produit (idProduit, nomProduit, prixRevientUnite, stock, nomCat)
VALUES (10, 'Chargeur sans fil', 30, 100, 'Electronique');

INSERT INTO Produit (idProduit, nomProduit, prixRevientUnite, stock, nomCat)
VALUES (11, 'Banane', 5, 50, 'Electronique');


-- Insertion d'une vente
-- INSERT INTO VENTE(idvente, idproduit, idsalle, revocable, offreunique, limite, descendante, prixdepart, datevente)
-- VALUES (0,1,0,0,0,0,0,15, TO_DATE('2024-12-02 18:50:00', 'YYYY-MM-DD HH24:MI:SS'));


-- Insertion dans la table Caracteristique (quelques caractéristiques fictives)
INSERT INTO Caracteristique (nomCar)
VALUES ('Étanchéité');

INSERT INTO Caracteristique (nomCar)
VALUES ('Batterie longue durée');

INSERT INTO Caracteristique (nomCar)
VALUES ('Charge rapide');

INSERT INTO Caracteristique (nomCar)
VALUES ('Ecran haute définition');

INSERT INTO Caracteristique (nomCar)
VALUES ('Connectivité Bluetooth');

-- Insertion dans la table CaracteristiqueProduit (relation entre produit et caractéristiques)
-- Exemple d'association d'une caractéristique à chaque produit
INSERT INTO CaracteristiqueProduit (idProduit, nomCar, valCar)
VALUES (1, 'Batterie longue durée',200);

INSERT INTO CaracteristiqueProduit (idProduit, nomCar, valCar)
VALUES (2, 'Charge rapide',300);

