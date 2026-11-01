package vn.ktx.dao;

import vn.ktx.config.Database;
import vn.ktx.model.UserSession;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

public final class AuthDao {
    public boolean needsSetup() throws SQLException {
        try (Connection c=Database.getConnection(); Statement s=c.createStatement();
             ResultSet r=s.executeQuery("SELECT COUNT(*) FROM TaiKhoan")) { r.next(); return r.getInt(1)==0; }
    }
    public void setup(String name, char[] password) throws SQLException {
        // Khóa cùng một tên trên server để hai cửa sổ khởi tạo không tạo hai quản trị viên.
        try (Connection c=Database.getConnection(); Statement lock=c.createStatement()) {
            try (ResultSet r=lock.executeQuery("SELECT GET_LOCK('ktx_initial_admin',10)")) {
                if (!r.next() || r.getInt(1)!=1) throw new SQLException("Không lấy được khóa khởi tạo.");
            }
            try {
                try (ResultSet r=lock.executeQuery("SELECT COUNT(*) FROM TaiKhoan")) {
                    r.next(); if(r.getInt(1)>0) throw new SQLException("Đã có tài khoản. Hãy đăng nhập.");
                }
                insert(c,name,password,"ADMIN",null);
            } finally { lock.executeQuery("SELECT RELEASE_LOCK('ktx_initial_admin')").close(); }
        }
    }
    public void create(UserSession actor, String name, char[] password, String role, String student) throws SQLException {
        if (!actor.isAdmin()) throw new SQLException("Chỉ quản trị viên được tạo tài khoản.");
        if (!java.util.Set.of("ADMIN","STAFF","STUDENT").contains(role)) throw new SQLException("Vai trò không hợp lệ.");
        if ("STUDENT".equals(role) && (student==null || student.isBlank())) throw new SQLException("Cần mã sinh viên.");
        try (Connection c=Database.getConnection()) { insert(c,name,password,role,"STUDENT".equals(role)?student:null); }
    }
    private void insert(Connection c,String name,char[] password,String role,String student) throws SQLException {
        if (name.isBlank() || name.length()>50 || password.length<8) throw new SQLException("Tên đăng nhập 1–50 ký tự; mật khẩu ít nhất 8 ký tự.");
        try (PreparedStatement p=c.prepareStatement("INSERT INTO TaiKhoan VALUES (?,?,?,?)")) {
            p.setString(1,name.trim()); p.setString(2,hash(password)); p.setString(3,role); p.setString(4,student); p.executeUpdate();
        }
    }
    public UserSession login(String name,char[] password) throws SQLException {
        try (Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement("SELECT MatKhau,VaiTro,MaSV FROM TaiKhoan WHERE TenDangNhap=?")) {
            p.setString(1,name.trim());
            try(ResultSet r=p.executeQuery()) {
                if(r.next() && verify(password,r.getString(1))) return new UserSession(name.trim(),r.getString(2),r.getString(3));
            }
        }
        throw new SQLException("Tên đăng nhập hoặc mật khẩu không đúng.");
    }
    public static String hash(char[] password) {
        byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
        return "210000:"+Base64.getEncoder().encodeToString(salt)+":"+Base64.getEncoder().encodeToString(derive(password,salt,210000));
    }
    public static boolean verify(char[] password,String stored) {
        try {
            String[] parts=stored.split(":");
            return MessageDigest.isEqual(Base64.getDecoder().decode(parts[2]),derive(password,Base64.getDecoder().decode(parts[1]),Integer.parseInt(parts[0])));
        } catch (RuntimeException e) { return false; }
    }
    private static byte[] derive(char[] password,byte[] salt,int rounds) {
        PBEKeySpec spec=new PBEKeySpec(password,salt,rounds,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (Exception e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
}
