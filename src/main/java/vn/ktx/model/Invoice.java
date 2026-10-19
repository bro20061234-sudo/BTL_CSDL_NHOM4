package vn.ktx.model;

import java.math.BigDecimal;

public class Invoice {
    private final String studentId;
    private final String studentName;
    private final String roomId;
    private final BigDecimal roomFee;
    private final BigDecimal serviceFee;
    private final BigDecimal monthlyParkingFee;
    private final BigDecimal extraParkingFee;
    private final BigDecimal total;

    public Invoice(String studentId, String studentName, String roomId, BigDecimal roomFee,
                   BigDecimal serviceFee, BigDecimal monthlyParkingFee,
                   BigDecimal extraParkingFee, BigDecimal total) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.roomId = roomId;
        this.roomFee = roomFee;
        this.serviceFee = serviceFee;
        this.monthlyParkingFee = monthlyParkingFee;
        this.extraParkingFee = extraParkingFee;
        this.total = total;
    }

    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getRoomId() { return roomId; }
    public BigDecimal getRoomFee() { return roomFee; }
    public BigDecimal getServiceFee() { return serviceFee; }
    public BigDecimal getMonthlyParkingFee() { return monthlyParkingFee; }
    public BigDecimal getExtraParkingFee() { return extraParkingFee; }
    public BigDecimal getTotal() { return total; }
}
