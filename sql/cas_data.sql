INSERT INTO adherents (
  id, nom, prenom, mail, login, password, date_de_depart, commentaires,
  mode_association, access_token, subnet, ip, chambre_id, created_at,
  updated_at, edminet, is_naina, mailinglist, mail_membership, ldap_login,
  datesignedhosting, datesignedadhesion
) VALUES
  (1, 'Dupont', 'Jean', 'jean.dupont@example.com', 'jdupont',
   'b39a61f16a4e11fa80580241f1d4aae8', '2025-12-31', 'Premier utilisateur',
   1, 'token_jdupont', '10.0.0.0/24', '10.0.0.10', 1001,
  '2025-01-01 10:00:00', '2025-01-02 10:00:00', 1, 0, 1, 1, 'ldap_jdupont',
  '2025-01-10 10:00:00', '2025-01-15 10:00:00'),
  (2, 'Martin', 'Marie', 'marie.martin@example.com', 'mmartin',
   'c2cc78ba8b1df908f563858b3095c7c7', '2026-06-30', 'Deuxième utilisateur',
   2, 'token_mmartin', '10.0.1.0/24', '10.0.1.20', 1002,
  '2025-02-01 11:00:00', '2025-02-02 11:00:00', 0, 1, 0, 2, 'ldap_mmartin',
  '2025-02-10 11:00:00', '2025-02-15 11:00:00');
