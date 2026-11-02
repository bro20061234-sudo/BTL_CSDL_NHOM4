package vn.ktx.model;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;

public final class ParkingFee {
    private ParkingFee() { }
    public record Quote(long dayBlocks,long nightBlocks) {
        public BigDecimal total() { return BigDecimal.valueOf(dayBlocks).multiply(BigDecimal.valueOf(3000)).add(BigDecimal.valueOf(nightBlocks).multiply(BigDecimal.valueOf(5000))); }
    }
    /** Mỗi khung [06h,18h) / [18h,06h hôm sau) có giao với lượt gửi được tính một lần. */
    public static Quote calculate(LocalDateTime in,LocalDateTime out) {
        if(in==null || out==null || !out.isAfter(in)) throw new IllegalArgumentException("Giờ lấy xe phải sau giờ gửi xe.");
        long start=slot(in);
        // Trừ 1 nano để không tính thêm khung bắt đầu đúng lúc lấy xe.
        long end=slot(out.minusNanos(1));
        long blocks=end-start+1,day=blocks/2;
        if(blocks%2!=0 && Math.floorMod(start,2)==0) day++;
        return new Quote(day,blocks-day);
    }
    private static long slot(LocalDateTime time) {
        long day=ChronoUnit.DAYS.between(LocalDate.of(2000,1,1),time.toLocalDate())*2;
        return time.toLocalTime().isBefore(LocalTime.of(6,0))?day-1:time.toLocalTime().isBefore(LocalTime.of(18,0))?day:day+1;
    }
}
