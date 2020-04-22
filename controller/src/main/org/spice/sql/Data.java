package org.spice.sql;

import javax.json.Json;
import javax.json.JsonWriter;
import javax.json.JsonObjectBuilder;
import javax.json.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Data {
    Connection con;
    private final String user = "testuser";
    private final String pass = "test623";
    private final String url = "jdbc:mysql://localhost:3306/storeback?serverTimezone=UTC";

    public Data() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection(url, user, pass);
        } catch(SQLException | ClassNotFoundException ex) {
            Logger lgr = Logger.getLogger(this.getClass().getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            con = null;
        }
    }

    public class Product {
        public final int id; // itemId
        public final String name;
        public final double price;

        protected Product(int id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        public JsonObject toJson() {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("id", id);
            if(name != null) builder.add("name", name);
            builder.add("price", price);
            return builder.build();
        }
    }

    public Product createProduct(String name, double price) throws SQLException {
        PreparedStatement ps =
            con.prepareStatement("insert into Products(name,price) values(?,?)",
                                 PreparedStatement.RETURN_GENERATED_KEYS);
        ps.setString(1, name);
        ps.setDouble(2, price);
        ps.executeUpdate();

        int id = -1;
        ResultSet rs = ps.getGeneratedKeys();
        if(rs.next()) {
            id = rs.getInt(1);
        }

        return new Product(id, name, price);
    }

    public Product getProductById(int _id) throws SQLException {
        PreparedStatement ps =
            con.prepareStatement("select itemId,name,price from Products where id=?");

        ps.setInt(1, _id);

        ResultSet rs = ps.executeQuery();

        int id = -1;
        String name = "";
        double price = 0;
        if(rs.next()) {
            id = rs.getInt(1);
            name = rs.getString(2);
            price = rs.getDouble(3);
        }

        return new Product(id, name, price);
    }

    public boolean deleteProduct(int id) throws SQLException {
        PreparedStatement ps =
            con.prepareStatement("delete from Products where itemId=?");

        ps.setInt(1, id);
        return ps.executeUpdate() > 0;
    }

    // From here on, all Person related JDBC functions will be inserted.
    // Adding Persons and Searching mostly important functions. 
    public class Person {
        public final int id; // personId
        public final String name;
        public final String credit;
        public final String address; // billingAddress

        protected Person(int id, String name, String credit, String address) {
            this.id = id;
            this.name = name;
            this.credit = credit;
            this.address = address;
        }

        public JsonObject toJson() {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("id", id);
            if(name != null) builder.add("name", name);
            if(credit != null) builder.add("credit", credit);
            if(address != null) builder.add("address", address);
            return builder.build();
        }
    }

    public Person createPerson(String name, String address, String credit) throws SQLException {
        PreparedStatement ps = con.prepareStatement("insert into Persons(name, credit, billingAddress) values(?,?,?)", 
                                PreparedStatement.RETURN_GENERATED_KEYS);
        ps.setString(1,name);
        ps.setString(2,address);
        ps.setString(3,credit);
        ps.executeUpdate();
        
        int cid = -1;
        ResultSet rs = ps.getGeneratedKeys();
        while(rs.next()) {
            cid = rs.getInt(1);
        }

        return new Person(cid, name, credit, address); //Added by Kenneth, setting up when users add the order.
    }

    public List<Person> searchCustomers(String name,
                                        String product) throws SQLException {
        StringBuilder qry = new StringBuilder("select distinct");
        qry.append(" P.personId, P.name, P.credit, P.billingAddress");
        qry.append(" from Orders O, Persons P, Products I");

        boolean first = true;

        if(name != null) {
            if(first) { first = false; qry.append(" where");}
            else { qry.append(" and"); }
            qry.append(" P.name regexp ?");
        }

        if(product != null) {
            if(first) { first = false; qry.append(" where");}
            else { qry.append(" and"); }
            qry.append(" O.personId=P.personId");
            qry.append(" and O.itemId=I.itemId");
            qry.append(" and I.name regexp ?");
        }

        PreparedStatement ps = con.prepareStatement(qry.toString());

        int idx = 1;
        if(name != null) ps.setString(idx++, name);
        if(product != null) ps.setString(idx++, product);

        ResultSet rs = ps.executeQuery();

        ArrayList<Person> ret = new ArrayList<Person>();

        while(rs.next()) {
            ret.add(new Person(rs.getInt(1),
                               rs.getString(2),
                               rs.getString(3),
                               rs.getString(4)));
        }

        return ret;
    }

    // Here will be the public class for Discounts.
    // All methods pertaining to discounts will be added here (adding, searching, etc).
    public class Discounts {
        public final int discId;
        public final Date createdDate;

        protected Discounts(int discId, Date createdDate) {
            this.discId = discId;
            this.createdDate = createdDate;
        }

        public JsonObject toJson() {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("discId", discId);
            builder.add("createdDate", createdDate.toString());
            return builder.build();
        }
    }

        //Creating discount codes, taking in only the generated int string.
        // Created the date here as a reference.
    public Discounts createDiscount() throws SQLException
    {
        PreparedStatement ps = con.prepareStatement("insert into Discounts() values()",
                            PreparedStatement.RETURN_GENERATED_KEYS);

        ps.executeUpdate();
            
        int id = -1;
        Date newDate = null;

        ResultSet rs = ps.getGeneratedKeys();
        
        if(rs.next()) {
            id = rs.getInt(1);
        }
                    
        return new Discounts(id, newDate);
    }

        // Searching Discount codes method. 
    public Discounts searchDiscount (int discountId) throws SQLException 
    {
        PreparedStatement ps = con.prepareStatement("select discountId,dateCreated from Discounts where id=?");
        ps.setInt(1, discountId);

        ResultSet rs = ps.executeQuery();

        int discountid = -1;
        Date dateCreated = null;

        if(rs.next()) {
            discountid = rs.getInt(1);
            dateCreated = rs.getDate(2);
        } 
            
        return new Discounts(discountid, dateCreated);
    }

    // Adding onto this, the Orders class.
    // below will be the functions that take in everything.
    public class Orders {
        public final int personId;
        public final int itemId;
        public final int discountId;
        public final int orderId;

        Orders(int orderId, int itemId, int discountId, int personId)
        {
            this.orderId = orderId;
            this.discountId = discountId;
            this.itemId = itemId;
            this.personId = personId;
        }

        public JsonObject toJson() {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("orderId", orderId);
            builder.add("itemId", itemId);
            builder.add("discountId", discountId);
            builder.add("personId", personId);
            return builder.build();
        }
    }

    public Orders createOrder(int itemId, int discountId, int personId) throws SQLException {
        PreparedStatement ps = con.prepareStatement("insert into Orders(itemId,discountId,personId) values(?,?,?)",
                            PreparedStatement.RETURN_GENERATED_KEYS);
        ps.setInt(1, itemId);
        ps.setInt(2, discountId);
        ps.setInt(3, personId);
        
        ps.executeUpdate();
            
        int id = -1;
        ResultSet rs = ps.getGeneratedKeys();
        
        if(rs.next()) {
            id = rs.getInt(1);
        }
                    
        return new Orders(id, itemId,discountId,personId);
    }
}
