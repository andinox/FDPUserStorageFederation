CREATE TABLE IF NOT EXISTS adherents (
    login VARCHAR(255) NOT NULL PRIMARY KEY,
    prenom VARCHAR(255),
    nom VARCHAR(255),
    mail VARCHAR(255),
    password VARCHAR(255)
);
