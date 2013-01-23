package com.aplana.timesheet.dao.entity;

import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.util.Date;

/**
 * @author rshamsutdinov
 * @version 1.0
 */
@Entity
@Table(name = "vacation")
public class Vacation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vacation_seq")
    @SequenceGenerator(name = "vacation_seq", sequenceName = "vacation_seq", allocationSize = 10)
    @Column(columnDefinition = "integer not null")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @ForeignKey(name = "fk_employee")
    private Employee employee;

    @Column(name = "begin_date", columnDefinition = "date not null")
    private Date beginDate;

    @Column(name = "end_date", columnDefinition = "date not null")
    private Date endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    @ForeignKey(name = "fk_vacation_type")
    private DictionaryItem type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    @ForeignKey(name = "fk_vacation_status")
    private DictionaryItem status;

    @Column(name = "comment", columnDefinition = "character(400)")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @ForeignKey(name = "fk_author")
    private Employee author;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public DictionaryItem getType() {
        return type;
    }

    public void setType(DictionaryItem type) {
        this.type = type;
    }

    public DictionaryItem getStatus() {
        return status;
    }

    public void setStatus(DictionaryItem status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Employee getAuthor() {
        return author;
    }

    public void setAuthor(Employee author) {
        this.author = author;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Vacation");
        sb.append("{id=").append(id);
        sb.append(", employee=").append(employee);
        sb.append(", beginDate=").append(beginDate);
        sb.append(", endDate=").append(endDate);
        sb.append(", type=").append(type);
        sb.append(", status=").append(status);
        sb.append(", comment='").append(comment).append('\'');
        sb.append(", author=").append(author);
        sb.append('}');
        return sb.toString();
    }
}