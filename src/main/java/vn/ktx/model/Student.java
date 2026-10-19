package vn.ktx.model;

import java.time.LocalDate;

public class Student {
    private final String id;
    private final String identityNumber;
    private final String fullName;
    private final LocalDate birthDate;
    private final String className;
    private final String hometown;
    private final String roomId;
    private final LocalDate admissionDate;

    public Student(String id, String identityNumber, String fullName, LocalDate birthDate,
                   String className, String hometown, String roomId, LocalDate admissionDate) {
        this.id = id;
        this.identityNumber = identityNumber;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.className = className;
        this.hometown = hometown;
        this.roomId = roomId;
        this.admissionDate = admissionDate;
    }

    public String getId() { return id; }
    public String getIdentityNumber() { return identityNumber; }
    public String getFullName() { return fullName; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getClassName() { return className; }
    public String getHometown() { return hometown; }
    public String getRoomId() { return roomId; }
    public LocalDate getAdmissionDate() { return admissionDate; }
}
