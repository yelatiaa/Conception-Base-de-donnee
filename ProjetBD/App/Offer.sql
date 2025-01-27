Begin;

-- On récupère l'email d'un utilisateur
SELECT email FROM Utilisateur WHERE email = ?;

-- On récupère une vente pour laquelle faire une offre
SELECT idVente from Vente;

-- On récupère les potentielles offres existantes pour cet utilisateur sur la vente
SELECT * FROM Offre WHERE idVente = ? AND email = ?;

-- On regarde si l'offre est unique
SELECT offreUnique FROM Vente WHERE idVente = ?;

-- Si la vente est unique et qu'il y a déjà une offre, on demande à choisir une autre vente pour l'offre

-- On regarde si la vente est descendante
SELECT descendante FROM Vente WHERE idVente = ?;

-- On récupère le produit mis en vente dans la vente choisie
SELECT idProduit FROM Vente WHERE idVente = ?;

COMMIT;
END;