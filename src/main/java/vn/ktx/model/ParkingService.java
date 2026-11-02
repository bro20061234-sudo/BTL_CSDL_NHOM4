package vn.ktx.model;

import vn.ktx.dao.ParkingDao;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Đồng hồ thực trong sản phẩm, có thể thay đồng hồ để kiểm thử các mốc giờ. */
public final class ParkingService {
    public record Entry(String id,String plate,String student,boolean monthly,LocalDateTime entered,BigDecimal monthlyFee) {
        public BigDecimal estimate(LocalDateTime now) {
            if(monthly) return monthlyFee;
            if(now.isBefore(entered)) return BigDecimal.ZERO;
            return ParkingFee.calculate(entered,now.isAfter(entered)?now:entered.plusNanos(1)).total();
        }
    }
    public record Receipt(Entry entry,LocalDateTime exited,BigDecimal amount) { }
    private static final ParkingService SHARED=new ParkingService(new ParkingDao(),Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")));
    private final ParkingDao dao;
    private final Clock clock;
    private final Map<String,Entry> casual=new LinkedHashMap<>();
    public ParkingService(ParkingDao dao,Clock clock) { this.dao=dao;this.clock=clock; }
    public static ParkingService shared() { return SHARED; }
    public LocalDateTime now() { return LocalDateTime.now(clock); }
    public static String canonical(String number) { return number==null?"":number.replaceAll("[\\s.\\-]","").toUpperCase(Locale.ROOT); }
    public synchronized List<Entry> active() throws SQLException {
        List<Entry> result=new ArrayList<>(dao.active());result.addAll(casual.values());result.sort(Comparator.comparing(Entry::entered).reversed().thenComparing(Entry::id));return result;
    }
    public synchronized Entry enter(String plate) throws SQLException {
        String key=canonical(plate);
        if(key.isBlank() || key.length()>20) throw new SQLException("Nhập biển số xe hợp lệ, tối đa 20 ký tự.");
        if(casual.values().stream().anyMatch(e->canonical(e.plate()).equals(key))) throw new SQLException("Xe này đang trong bãi, không được gửi trùng.");
        LocalDateTime time=now().withNano(0);
        Entry entry=dao.enter(plate.trim().toUpperCase(Locale.ROOT),time);
        if(entry!=null) return entry;
        entry=new Entry("C:"+UUID.randomUUID(),plate.trim().toUpperCase(Locale.ROOT),"Khách / vé lượt",false,time,BigDecimal.ZERO);
        casual.put(entry.id(),entry);return entry;
    }
    public synchronized Receipt leave(String id) throws SQLException {
        LocalDateTime time=now();
        if(id.startsWith("M:")) {
            try { return dao.leave(Long.parseLong(id.substring(2)),time); }
            catch(NumberFormatException invalid) { throw new SQLException("Mã lượt gửi không hợp lệ."); }
        }
        Entry entry=casual.get(id);
        if(entry==null) throw new SQLException("Xe đã lấy hoặc không còn trong bãi.");
        if(time.isBefore(entry.entered())) throw new SQLException("Đồng hồ hiện tại sớm hơn giờ gửi xe.");
        BigDecimal amount=entry.estimate(time);casual.remove(id);return new Receipt(entry,time,amount);
    }
}
