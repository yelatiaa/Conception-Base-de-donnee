
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class Main {

    static Connection connection;
    static String url = "jdbc:oracle:thin:@oracle1.ensimag.fr:1521:oracle1";
    static boolean end = false;
    static boolean connected = false;
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws SQLException {

        while (!connected) {
            //System.out.print("Login : ");
            String login = "pignierb";//scanner.nextLine();
            //System.out.print("Password : ");
            String password = "pignierb";//scanner.nextLine();
            try {
                connection = DriverManager.getConnection(url, login, password);
                connection.setAutoCommit(false);
                connected = true;
            } catch (SQLException e) {
                System.out.println("Connection error : wrong login or password");
            }
        }

        String[] tables = {
            "Offre", "CaracteristiqueProduit", "Caracteristique",
            "VenteDescendante", "VenteLimite", "Vente",
            "Produit", "SALLE", "Categorie", "Utilisateur"
        };
        for (String table : tables) {
            if (tableExists(connection, table)) {
                dropTable(connection, table);
            }
        }

        runSQLFile("tables.sql");
        runSQLFile("populate.sql");
        connection.commit();

        while (!end) {
            System.out.println("\n=== MENU PRINCIPAL ===");
            System.out.println("1. Créer une salle");
            System.out.println("2. Ajouter une vente à une salle");
            System.out.println("3. Créer une offre");
            System.out.println("4. Afficher les résultats");
            System.out.println("5. Quitter");
            System.out.print("Votre choix : ");

            int input = 0;
            try {
                input = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Entrée invalide. Veuillez entrer un nombre");
            }

            switch (input) {
                case 1:
                    create_room();
                    connection.commit();
                    break;
                case 2:
                    int nbr_salle = getTableLenght("Salle");
                    if (nbr_salle == 0) {
                        System.out.println("Il n'y a pas de salle, il faut d'abord en créer une.");
                        break;
                    }
                    int idSalle = chooseSalle();

                    String nomCat = getStringFromStatement("SELECT nomCat FROM Salle WHERE idSalle = ?", new String[]{String.valueOf(idSalle)},"nomCat");

                    ArrayList<Integer> products_chosen = chooseProducts(nomCat,false);

                    PreparedStatement pst_ventesFromSalle = connection.prepareStatement("SELECT * FROM Vente WHERE idSalle = ?");
                    pst_ventesFromSalle.setInt(1, idSalle);
                    ResultSet ventesFromSalle = pst_ventesFromSalle.executeQuery();
                    int nbr_vente = getTableLenght("Vente");
                    if (ventesFromSalle.next()) {
                        int Revocable = ventesFromSalle.getInt("REVOCABLE");
                        int Offreunique = ventesFromSalle.getInt("OFFREUNIQUE");
                        int limite = ventesFromSalle.getInt("LIMITE");
                        int descendante = ventesFromSalle.getInt("DESCENDANTE");
                        creer_vente(nbr_vente, products_chosen.get(0),idSalle,Revocable,Offreunique,limite,descendante);
                    }else{
                        System.out.println("Pas de vente disponible");
                    }
                    ventesFromSalle.close();
                    pst_ventesFromSalle.close();
                    connection.commit();
                    break;
                case 3:
                    create_offer();
                    connection.commit();
                    break;
                case 4:
                    PreparedStatement pst_ventes = connection.prepareStatement("SELECT * FROM Vente");
                    ResultSet ventes = pst_ventes.executeQuery();
                    boolean venteExists = false;
                    while (ventes.next()){
                        venteExists = true;
                        String idVente = ventes.getString("idVente");
                        String idProduit = ventes.getString("idProduit");
                        String nomProduit = getStringFromStatement("SELECT nomProduit FROM Produit WHERE idProduit = ?",new String[]{idProduit},"nomProduit");
                        int revocable = ventes.getInt("REVOCABLE");
                        int isDescendante = getIntFromStatement("SELECT descendante FROM Vente WHERE idVente = ?", new String[]{idVente}, "descendante");
                        if (isDescendante == 0){ // Vente montante

                            PreparedStatement pstmt = connection.prepareStatement("""
                            SELECT o.email,
                                   o.prixOffre,
                                   p.prixRevientUnite,
                                   o.quantite,
                                   (o.prixOffre - p.prixRevientUnite) * o.quantite AS profitVendeur
                            FROM Offre o
                            JOIN Vente v ON o.idVente = v.idVente
                            JOIN Produit p ON v.idProduit = p.idProduit
                            WHERE o.idVente = ?
                            ORDER BY (o.prixOffre - p.prixRevientUnite) * o.quantite DESC
                            FETCH FIRST 1 ROWS ONLY""");
                            pstmt.setString(1, idVente);
                            ResultSet rs = pstmt.executeQuery();
                            if (rs.next()) {
                                if (revocable == 1 && rs.getInt("profitVendeur") < 0){
                                    System.out.println("La vente montante étant revocable et la meilleur offre étant à perte pour le vendeur, personne ne remporte la vente pour le moment");
                                }else{
                                    System.out.println("Gagnant de l'enchère du produit "+nomProduit+":");
                                    System.out.println("    Email : " + nomProduit);
                                    System.out.println("    Prix d'achat : " + rs.getInt("prixOffre"));
                                    System.out.println("    Quantité Vendue : " + rs.getInt("quantite"));
                                    System.out.println("    Profit du Vendeur : " + rs.getInt("profitVendeur"));
                                }
                            } else {
                                System.out.println("Il n'y a pas d'offre pour la vente de "+nomProduit);
                            }
                            pstmt.close();
                            rs.close();
                        }else{ // Vente descendante
                            PreparedStatement pst_offreFromVente = connection.prepareStatement("SELECT * FROM Offre WHERE idVente = ?");
                            pst_offreFromVente.setString(1, idVente);
                            ResultSet offre = pst_offreFromVente.executeQuery();
                            boolean isThereOffer = false;
                            while (offre.next()){
                                isThereOffer = true;
                                System.out.println("Offre de "+offre.getString("email"));
                                System.out.println("    Produit : "+nomProduit);
                                System.out.println("    Prix d'achat à l'unité : " + offre.getInt("prixOffre"));
                                System.out.println("    Quantité Vendue : " + offre.getInt("quantite"));
                            }
                            if (!isThereOffer){
                                System.out.println("Il n'y a pas d'offre pour la vente de "+nomProduit);
                            }
                            offre.close();
                            pst_offreFromVente.close();
                        }
                    }
                    if (!venteExists){
                        System.out.println("Il n'y a pas de vente pour le moment");
                    }
                    ventes.close();
                    pst_ventes.close();
                    break;
                case 5:
                    close_connection();
                    end = true;
                    break;
                default:
                    System.out.println("Choix invalide. Veuillez entrer un nombre entre 1 et 5.");
                    break;
            }
        }
    }
    public static int parseInt(String string) {
        int input = 0;
        try {
            input = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            System.out.println("Entrée invalide. Veuillez entrer un nombre");
        }
        return input;
    }

    public static int chooseSalle() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rset = statement.executeQuery("SELECT * FROM Salle");
        System.out.println("Choisissez une salle dans cette liste :");
        int i = 1;
        ArrayList<Integer> map = new ArrayList<>();
        while (rset.next()) {
            int idSalle = rset.getInt("idSalle");
            map.add(idSalle);
            System.out.println(i + ". " + rset.getString("nomCat"));
            i++;
        }
        rset.close();
        statement.close();
        System.out.print("> ");
        int idx = parseInt(scanner.nextLine());
        return map.get(idx - 1);
    }
    public static int choose(String choix1, String choix2) {
        System.out.println("1 : " + choix1);
        System.out.println("* : " + choix2);
        System.out.print("Votre choix : ");
        return parseInt(scanner.nextLine()) == 1 ? 1 : 0;
    }

    public static int chooseVente() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rset = statement.executeQuery("SELECT * FROM Vente");
        System.out.println("Choisissez une vente dans cette liste :");
        int i = 1;
        ArrayList<Integer> map = new ArrayList<>();
        while (rset.next()) {
            int idVente = rset.getInt("idVente");
            map.add(idVente);
            String nomProduit = getStringFromStatement("SELECT nomProduit FROM Produit WHERE idProduit = ?", new String[]{String.valueOf(rset.getInt("idProduit"))}, "nomProduit");
            System.out.println(i + ". " + nomProduit);
            i++;
        }
        rset.close();
        statement.close();
        System.out.print("> ");
        int idx = parseInt(scanner.nextLine());
        return map.get(idx - 1);
    }

    public static String getKeyEntryFromTable(String tableName, String key) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rset = statement.executeQuery("SELECT * FROM " + tableName);

        int i = 1;
        ArrayList<String> map = new ArrayList<>();
        while (rset.next()) {
            String keyString = rset.getString(key);
            map.add(keyString);
            System.out.println(i + ". " + keyString);
            i++;
        }
        rset.close();
        statement.close();
        System.out.print("> ");
        int idx = parseInt(scanner.nextLine());
        return map.get(idx - 1);
    }

    public static int getIntFromStatement(String statement, String[] paramKeys, String returnKey) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(statement);
        for (int i = 0; i < paramKeys.length; i++) {
            pstmt.setString(i + 1, paramKeys[i]);
        }
        ResultSet res = pstmt.executeQuery();
        int ret = -99;
        if (res.next()) {
            ret = res.getInt(returnKey);
        }
        res.close();
        return ret;
    }

    public static String getStringFromStatement(String statement, String[] paramKeys, String returnKey) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(statement);
        for (int i = 0; i < paramKeys.length; i++) {
            pstmt.setString(i + 1, paramKeys[i]);
        }
        ResultSet res = pstmt.executeQuery();
        String ret = null;
        if (res.next()) {
            ret = res.getString(returnKey);
        }
        res.close();
        return ret;
    }

    public static Date getDateFromStatement(String statement, String[] paramKeys, String returnKey) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(statement);
        for (int i = 0; i < paramKeys.length; i++) {
            pstmt.setString(i + 1, paramKeys[i]);
        }
        ResultSet res = pstmt.executeQuery();
        Date ret = null;
        if (res.next()) {
            ret = res.getDate(returnKey);
        }
        res.close();
        return ret;
    }

    public static void create_offer() throws SQLException {
        if (getTableLenght("Vente") == 0) {
            System.out.println("Pas de vente disponible. Il faut d'abord créer une salle de vente");
            return;
        }
        String email = getKeyEntryFromTable("UTILISATEUR", "email");
        String idVente = null;
        boolean vente_selected = false;
        PreparedStatement pstmt;
        /* Traiter si la vente elle est à utilisateur unique*/
        while (!vente_selected) {
            idVente = String.valueOf(chooseVente());
            String ret = getStringFromStatement("SELECT * FROM Offre WHERE idVente = ? AND email = ?", new String[]{idVente, email}, "email");
            int venteUnique = getIntFromStatement("SELECT offreUnique FROM Vente WHERE idVente = ?", new String[]{idVente}, "offreUnique");
            if (ret != null && venteUnique == 1) {
                System.out.println("Attention : cette vente est limitée à une offre unique. Vous ne pouvez pas soumettre une autre offre pour cette vente.");
                System.out.println("Veuillez choisir une autre vente.");
            } else {
                vente_selected = true;
            }
        }


        /*verifier la condition sur la quantité*/
        boolean quantite_ok = false;
        int quantite = 0;

        String idProduit = getStringFromStatement("SELECT idProduit FROM Vente WHERE idVente = ?", new String[]{idVente}, "idProduit");


        boolean isDescendante = getIntFromStatement("SELECT descendante FROM Vente WHERE idVente = ?", new String[]{idVente}, "descendante") == 1;


        System.out.println("Quelle quantité souhaitez-vous acheter ?");
        while (!quantite_ok) {
            System.out.print("> ");
            quantite = parseInt(scanner.nextLine());
            int stock;
            if (isDescendante) {
                stock = getIntFromStatement("SELECT quantiteRestante FROM VenteDescendante WHERE idVente = ?", new String[]{idVente}, "quantiteRestante");
            } else {
                stock = getIntFromStatement("SELECT stock FROM Produit WHERE idProduit = ?", new String[]{idProduit}, "stock");
            }
            if (quantite > stock) {
                System.out.println("La quantité demandée dépasse le stock disponible (" + stock + "). Réessayez.");
            } else {
                quantite_ok = true;

                if (isDescendante){
                    String sql = "UPDATE VenteDescendante SET quantiteRestante = quantiteRestante - ? WHERE idVente = ?";
                    pstmt = connection.prepareStatement(sql);
                    pstmt.setInt(1, quantite);
                    pstmt.setString(2, idVente);
                    pstmt.executeQuery();
                }
            }
        }

        boolean date_ok = false;
        Date date_offre = null;
        while (!date_ok) {
            date_offre = new Date(System.currentTimeMillis());
            boolean estLimite = getIntFromStatement("SELECT limite FROM Vente WHERE idVente = ?", new String[]{idVente}, "limite") == 1;
            if (estLimite) {
                Date dateFinVente = getDateFromStatement("SELECT dateHeureFin FROM VenteLimite WHERE idVente = ?", new String[]{idVente}, "dateHeureFin");
                if (date_offre.after(dateFinVente)) {
                    System.out.println("Une vente limitée ne peut pas accepter une offre après la date de fin de la vente.");
                    return;
                } else {
                    date_ok = true;
                }
            } else {
                Date resultDate = getDateFromStatement("SELECT MAX(dateHeureOffre) AS derniereDate FROM Offre WHERE idVente = ?", new String[]{idVente}, "derniereDate");
                if (resultDate == null){
                    date_ok = true;
                }else{
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(resultDate);
                    calendar.add(Calendar.MINUTE, 10);
                    Date updatedSqlDate = new Date(calendar.getTimeInMillis());
                    if (date_offre.after(updatedSqlDate)) {
                        System.out.println("Pour une vente non limitée, une offre ne peut pas dépasser 10 minutes après la dernière offre.");
                    } else {
                        date_ok = true;
                    }
                }
            }
        }
        int prixUnite;
        if(isDescendante){
            Date debutVente = getDateFromStatement("SELECT datevente FROM Vente WHERE idVente = ?", new String[]{idVente}, "datevente");
            long timestamp1 = debutVente.getTime();
            long timestamp2 = date_offre.getTime();

            long differenceInMillis = Math.abs(timestamp2 - timestamp1);
            long diff = TimeUnit.MILLISECONDS.toMinutes(differenceInMillis);

            int diminutionRateUnite = getIntFromStatement("SELECT diminutionRate FROM VenteDescendante WHERE idVente = ?", new String[]{idVente}, "diminutionRate");

            int prixDepartUnite = getIntFromStatement("SELECT prixDepart FROM Vente WHERE idVente = ?", new String[]{idVente}, "prixDepart");

            prixUnite = prixDepartUnite - diminutionRateUnite * (int)diff;


            boolean revocable = getIntFromStatement("SELECT revocable FROM Vente WHERE idVente = ?", new String[]{idVente}, "revocable") == 1;
            int prixrevientunite = getIntFromStatement("SELECT prixrevientunite FROM Produit WHERE idProduit = ?", new String[]{idProduit}, "prixrevientunite");

            if (prixUnite < 0){
                System.out.println("Votre offre a été refusée, car la vente est expirée");
                return;
            }
            if(revocable && prixUnite < prixrevientunite){
                System.out.println("Votre offre a été refusée, car vente révocable et offre pas assez rentable pour le vendeur");
                return;
            }
            System.out.println("Votre offre a été faite de prix de "+prixUnite+" par pièce");

        }else{
            int prixDepartLot = getIntFromStatement("SELECT prixDepart FROM Vente WHERE idVente = ?", new String[]{idVente}, "prixDepart");
            System.out.println("Donne moi un prix à l'unité pour l'offre :");
            prixUnite = parseInt(scanner.nextLine());

            int lastPrice = getIntFromStatement("SELECT * FROM Offre WHERE idVente = ? ORDER BY prixOffre DESC FETCH FIRST 1 ROWS ONLY\n", new String[]{idVente}, "prixOffre");

            boolean noLastOffer = lastPrice == -99;
            if(noLastOffer){
                if((prixUnite * quantite) < prixDepartLot ){
                    System.out.println("Votre offre a été refusée car le prix d'achat est inférieur au prix de départ ("+prixDepartLot+")");
                    return;
                }
            }else{
                int lastQuantite = getIntFromStatement("SELECT * FROM Offre WHERE idVente = ? ORDER BY quantite DESC FETCH FIRST 1 ROWS ONLY\n", new String[]{idVente}, "quantite");

                if ((prixUnite * quantite) < (lastPrice * lastQuantite)){
                    System.out.println("Votre offre a été refusée car le prix d'achat est inférieur au prix d'achat de la dernière offre");
                    return;
                }
            }
        }


        pstmt = connection.prepareStatement("INSERT INTO OFFRE VALUES (?,?,?,?,?)");
        pstmt.setString(1, email);
        pstmt.setString(2, idVente);
        pstmt.setInt(3, quantite);
        pstmt.setDate(4, date_offre);
        pstmt.setInt(5, prixUnite);
        pstmt.executeUpdate();
        pstmt.close();
    }

    public static int getTableLenght(String tableName) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet res = statement.executeQuery("SELECT COUNT(*) FROM " + tableName);
        int lenght = 0;
        if (res.next()) {
            lenght = res.getInt(1);
        }
        statement.close();
        return lenght;
    }

    public static ArrayList<Integer> chooseProducts(String categorieName, boolean multiple) throws SQLException {
        PreparedStatement pst_produits = connection.prepareStatement("SELECT * FROM PRODUIT WHERE nomCat = ? AND idProduit NOT IN (SELECT idProduit FROM Vente)");
        pst_produits.setString(1, categorieName);
        ResultSet rset = pst_produits.executeQuery();
        if (multiple){
            System.out.println("Choisissez un ou plusieurs produits dans cette liste : (séparé par des virgules)");
        }else{
            System.out.println("Choisissez un produit dans cette liste : ");
        }
        boolean products_ok = false;
        ArrayList<Integer> products_chosen = null;
        while(!products_ok){
            int i = 1;
            ArrayList<String> products_displayed = new ArrayList<>();
            while (rset.next()) {
                products_displayed.add(rset.getString("idProduit"));
                System.out.println(i + ". " + rset.getString("NOMPRODUIT"));
                i++;
            }
            System.out.print("> ");
            String[] splitString = scanner.nextLine().split(",");
            if (multiple) {
                products_ok = splitString.length > 0;
            }else {
                products_ok = splitString.length == 1;
            }
            products_chosen = new ArrayList<>();
            for (String numStr : splitString) {
                products_chosen.add(parseInt(products_displayed.get(Integer.parseInt(numStr.trim()) - 1)));
            }
        }

        return products_chosen;
    }
    public static void create_room() throws SQLException {
        System.out.println("\n=== Création d'une salle === \n");
        System.out.println("Choisissez une catégorie d'objet dans cette liste :");
        String nomcat = getKeyEntryFromTable("categorie", "nomcat");

        int nbr_salle = getTableLenght("Salle");

        PreparedStatement pst_insertSalle = connection.prepareStatement("INSERT INTO SALLE VALUES (?,?)");
        pst_insertSalle.setInt(1, nbr_salle);
        pst_insertSalle.setString(2, nomcat);
        pst_insertSalle.executeQuery();
        pst_insertSalle.close();



        int descendante = choose("Vente descendante", "Vente montante");
        int limitee = choose("Vente limitée", "Vente illimitée");
        int revocable = choose("Vente revocable", " Vente non revocable");
        int offreUnique = choose("Vente à offre unique", " Vente à offre à volonté");

        int nbr_vente = getTableLenght("VENTE");
        ArrayList<Integer> products_chosen = chooseProducts(nomcat,true);
        for (Integer idProduit : products_chosen) {
            creer_vente(nbr_vente, idProduit, nbr_salle, revocable, offreUnique, limitee, descendante);
            nbr_vente++;
        }
    }

    public static Date chooseDate(){
        System.out.println("La vente étant limitée, veuillez entrer sa date et son heure de fin au format YYYY-MM-DD HH:MM:SS (exemple : 2023-11-29 15:20:16) :");
        Date parsed_date;
        while (true) {
            System.out.print("> ");
            String userInput = scanner.nextLine();
            if (isDateConform(userInput)) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    java.util.Date utilDate = formatter.parse(userInput);
                    parsed_date = new Date(utilDate.getTime());

                    if (parsed_date.before(Date.valueOf(LocalDate.now()))){
                        System.out.println("Vous ne pouvez pas créer de vente dans le passé ");
                        throw new Exception();
                    }
                    break;
                } catch (Exception e) {
                    System.out.println("Erreur : La date est au bon format mais invalide. Veuillez réessayer.");
                }
            } else {
                System.out.println("Format incorrect. Veuillez réessayer.");
            }
        }
        return parsed_date;
    }

    private static void creer_vente(int nbr_vente, int idProduit, int nbr_salle, int revocable, int offreUnique, int limitee, int descendante) throws SQLException {
        PreparedStatement pst_vente = connection.prepareStatement("INSERT INTO VENTE VALUES (?,?,?,?,?,?,?,?,?)");

        pst_vente.setInt(1, nbr_vente);
        pst_vente.setInt(2, idProduit);
        pst_vente.setInt(3, nbr_salle);
        pst_vente.setInt(4, revocable);
        pst_vente.setInt(5, offreUnique);
        pst_vente.setInt(6, limitee);
        pst_vente.setInt(7, descendante);

        String nomProduit = getStringFromStatement("SELECT nomProduit FROM Produit WHERE idProduit = ?", new String[]{String.valueOf(idProduit)}, "nomProduit");
        int stockProduit = getIntFromStatement("SELECT stock FROM Produit WHERE idProduit = ?",new String[]{String.valueOf(idProduit)}, "stock");
        int prix;
        if (descendante == 1){
            System.out.println("Donne moi un prix de départ à l'unité pour le produit " +nomProduit + " : ");
            System.out.print("> ");
            prix = parseInt(scanner.nextLine());
        }else{
            System.out.println("Donne moi un prix de départ pour ce lot de "+ stockProduit + " " +nomProduit + " : ");
            System.out.print("> ");
            prix = parseInt(scanner.nextLine());
        }
        pst_vente.setInt(8, prix);
        Date currentDate = new Date(System.currentTimeMillis());
        pst_vente.setDate(9, currentDate);
        pst_vente.executeQuery();
        pst_vente.close();

        if (limitee == 1) {
            PreparedStatement pst_venteLimite = connection.prepareStatement("INSERT INTO VENTELIMITE VALUES (?,?)");
            pst_venteLimite.setInt(1, nbr_vente);
            pst_venteLimite.setDate(2, chooseDate());
            pst_venteLimite.executeQuery();
            pst_venteLimite.close();
        }
        if (descendante == 1) {
            PreparedStatement pst_venteDesc = connection.prepareStatement("INSERT INTO VENTEDESCENDANTE VALUES (?,?,?)");
            pst_venteDesc.setInt(1, nbr_vente);
            System.out.println("La vente étant descendante, veuillez entrer le montant que devra perdre le prix toutes les 10 minutes.");
            int diminution_rate = 0;
            while (true){
                System.out.print("> ");
                diminution_rate = Integer.parseInt(scanner.nextLine());
                if (diminution_rate < prix){
                    break;
                }
                System.out.println("Ce montant doit être strictement inférieur au prix.");

            }
            pst_venteDesc.setInt(2, diminution_rate);
            PreparedStatement res = connection.prepareStatement("SELECT STOCK FROM PRODUIT WHERE idProduit = ?");
            res.setInt(1, idProduit);
            ResultSet rset = res.executeQuery();
            int stock = 0;
            if (rset.next()) {
                stock = rset.getInt(1);
            }
            res.close();
            rset.close();
            pst_venteDesc.setInt(3, stock);
            pst_venteDesc.executeQuery();
            pst_venteDesc.close();
        }
    }

    private  static boolean isDateConform(String date){
        String regex = "^(\\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]) ([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$";
        return date.matches(regex);
    }
    // Vérifie si une table existe
    private static boolean tableExists(Connection conn, String tableName) {
        try{
            PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM all_tables WHERE table_name = ?");
            stmt.setString(1, tableName.toUpperCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    // Supprime une table
    private static void dropTable(Connection conn, String tableName) {
        String query = "DROP TABLE " + tableName + " CASCADE CONSTRAINTS";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(query);
            System.out.println("Table " + tableName + " supprimée.");
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression de la table " + tableName + ": " + e.getMessage());
        }
    }

    public static void close_connection() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void runSQLFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            StringBuilder sqlBuilder = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                // Ignore les lignes vides ou les commentaires
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }

                sqlBuilder.append(line);
                // Vérifie si la ligne se termine par un point-virgule
                if (line.endsWith(";")) {
                    sqlBuilder.deleteCharAt(sqlBuilder.length() - 1); // Retirer le ;
                    String sqlStatement = sqlBuilder.toString();
                    sqlBuilder.setLength(0); // Réinitialise le builder pour la prochaine commande

                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sqlStatement); // Exécute la commande SQL
                        System.out.println("Exécuté : " + sqlStatement);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);

                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier SQL : " + e.getMessage());
        }
    }
}
