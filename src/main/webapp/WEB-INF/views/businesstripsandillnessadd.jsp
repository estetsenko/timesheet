<%@page contentType="text/html" pageEncoding="UTF-8" %>

<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<html>
<head>
    <title>
        <c:choose>
                <c:when test="${reportId == null}">
                    <fmt:message key="businesstripsandillnessadd"/>
                </c:when>
            <c:when test="${reportId != null}">
                <fmt:message key="businesstripsandillnessedit"/>
            </c:when>
        </c:choose>

    </title>
    <link rel="stylesheet" type="text/css" href="<%= request.getContextPath()%>/resources/css/businesstripsandillnessadd.css">
    <script type="text/javascript">

        dojo.require("dijit.form.Select");
        dojo.require("dijit.form.DateTextBox");
        dojo.require("dijit.form.TextBox");
        dojo.require(CALENDAR_EXT_PATH);

        var illnessReportType = 6;
        var businessTripReportType = 7;

        var businesstrip_project = 55;
        var businesstrip_notproject = 56;

        var projectUndefined = -1;

        var errors;


        function getEmployeeId() {
            if (dojo.byId("employeeId")) {
                return parseInt(dojo.byId("employeeId").value);
            } else {
                return ${employeeId}
            }
        }

        initCurrentDateInfo(getEmployeeId());

        dojo.declare("Calendar", com.aplana.dijit.ext.SimpleCalendar, {
            getEmployeeId: getEmployeeId
        });

        dojo.declare("DateTextBox", dijit.form.DateTextBox, {
            popupClass: "Calendar",
            datePattern: 'dd.MM.yyyy'
        });

        dojo.ready(function () {
            window.focus();
            updateView();

            require(["dojo/on"], function(on){
                on.once(dojo.byId("create"), "click", function(){
                    submitform();
                });
            });
        });

        function updateView(){
            var obj = dojo.byId("reportType");
            var willBeDisplayedId = null;
            if (obj.target == null) {
                willBeDisplayedId = obj.value;
            }
            else {
                willBeDisplayedId = obj.target.value;
            }

            if (willBeDisplayedId == businessTripReportType){
                showBusinessTrips();
                updateProject();
            }
            else if (willBeDisplayedId == illnessReportType){
                showIllnesses();
            }
            document.getElementById("headerName").innerHTML = getHeader(willBeDisplayedId);
        }

        function getHeader(willBeDisplayedId) {
            if (${reportId == null}) {
                return "Создание " + getReportName();
            } else {
                return "Редактирование " + getReportName();
            }
        }

        function showBusinessTrips(){
            document.getElementById("illness").className = 'off';
            document.getElementById("businesstrip").className = 'creationform';
        }

        function showIllnesses(){
            document.getElementById("illness").className = 'creationform';
            document.getElementById("businesstrip").className = 'off';
        }

        function submitform(){
            if (validate()){
                if (${reportId == null}){
                    var employeeId = getEmployeeId();
                    mainForm.action = "<%=request.getContextPath()%>/businesstripsandillnessadd/tryAdd/" + employeeId;
                } else {
                    mainForm.action = "<%=request.getContextPath()%>/businesstripsandillnessadd/trySave/" + "${reportId}";
                }
                mainForm.submit();
            }
        }

        function cancelform(){
//            закоментированно до лучших времён
            <%--var reportType = parseInt(dojo.byId("reportType").value);--%>
            <%--var employeeId = parseInt(dojo.byId("employeeId").value);--%>
            <%--if (employeeId > 0) {--%>
                <%--switch (reportType) {--%>
                    <%--case illnessReportType:--%>
                        <%--mainForm.action = "<%=request.getContextPath()%>/businesstripsandillness/illness/" + employeeId;--%>
                        <%--break;--%>
                    <%--case businessTripReportType :--%>
                        <%--mainForm.action = "<%=request.getContextPath()%>/businesstripsandillness/businesstrip/" + employeeId;--%>
                        <%--break;--%>
                    <%--default: mainForm.action = "<%=request.getContextPath()%>/businesstripsandillness/";--%>
                <%--}--%>
            <%--} else {--%>
                mainForm.action = "<%=request.getContextPath()%>/businesstripsandillness/";
//            }

            mainForm.submit();
        }

        function validate(){
            delete errors;
            errors = new Array();
            if (checkRequired() && checkDates()){
                return true;
            } else {
                showErrors();
                return false;
            }
        }

        function showErrors(){
            document.getElementById("errorboxdiv").className = 'fullwidth onblock errorbox';
            var errortext = "";
            for (var errorindex in errors) {
                errortext = errortext + errors[errorindex] + "\n";
            }
            document.getElementById("errorboxdiv").firstChild.nodeValue = errortext;
        }

        function checkRequired(){
            if (checkCommon()){
                if (document.getElementById("reportType").value == illnessReportType){
                    return checkIllness();
                } else if (document.getElementById("reportType").value == businessTripReportType){
                    return checkBusinessTrip();
                } else {
                    errors.push("Выбран неизвестный тип отчета!");
                    return false;
                }
            } else {
                errors.push("Необходимо правильно заполнить все поля!");
                return false;
            }
        }

        function checkBusinessTrip(){
            var businessTripType = document.getElementById("businessTripType").value;
            if (businessTripType != null){
                if (businessTripType == businesstrip_project){
                    var projectId = document.getElementById("projectId").value;
                    if (projectId == null || projectId == projectUndefined || projectId == "0"){
                        errors.push("Для проектной командировки необходимо выбрать проект!");
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            } else {
                errors.push("Не выбран тип командировки!");
                return false;
            }
        }

        function checkIllness(){
            return document.getElementById("reason").value != null;
        }

        function checkCommon(){
            return (document.getElementById("reportType").value != null && document.getElementById("beginDate").value != null &&
                    document.getElementById("endDate").value != null);
        }

        function checkDates(){
            var beginDate = parseDate(document.getElementById("beginDate").value);
            var endDate = parseDate(document.getElementById("endDate").value);

            if (endDate >= beginDate){
                return true;
            } else {
                var reportName = getReportName();
                errors.push("Дата окончания " + reportName + " не может быть раньше даты начала!");
                return false;
            }

        }

        function getReportName(){
            var reportType = dojo.byId("reportType").value;
            reportType = parseInt(reportType);
            switch (reportType) {
                case illnessReportType: return"больничного";
                case businessTripReportType : return "командировки";
                default: return "";
            }
        }

        function parseDate(dateString){
            var dateParts = dateString.split(".");
            return new Date(dateParts[2], (dateParts[1] - 1), dateParts[0]);
        }

        function updateProject() {
            var businessTripType = dojo.byId("businessTripType").value;
            if (businessTripType == businesstrip_notproject){
                document.getElementById("businesstripproject").className = 'off';
                document.getElementById("projectId").disabled = true;
            }
            else {
                document.getElementById("businesstripproject").className = 'onblock';
                document.getElementById("projectId").disabled = false;
                var beginDate = dojo.byId("beginDate").value;
                var endDate = dojo.byId("endDate").value;
                var projectIdElement = dojo.byId("projectId");
                var employeeId = getEmployeeId();
                projectIdElement.innerHTML =
                        "<img src=\"<c:url value="/resources/img/loading_small.gif"/>\"/>";

                dojo.xhrGet({
                    url: "<%= request.getContextPath()%>/businesstripsandillnessadd/getprojects/" + employeeId +"/"+ beginDate + "/" + endDate + "/",
                    handleAs: "json",

                    load: function(data) {
                        updateProjectList(data);
                    },

                    error: function(error) {
                        projectIdElement.setAttribute("class", "error");
                        projectIdElement.innerHTML = error;
                    }
                });
            }
        }

        function updateProjectList(obj){
            var projectIdElement = dojo.byId("projectId");

            for (var projectindex in obj) {
                var projectOption = dojo.doc.createElement("option");
                dojo.attr(projectOption, {
                    value:obj[projectindex].id
                });
                projectOption.title = obj[projectindex].value;

                projectOption.innerHTML = obj[projectindex].value;
                projectIdElement.appendChild(projectOption);
            }
            sortSelectOptions(projectIdElement);
            if (${businesstripsandillnessadd.projectId != null}) {
                projectIdElement.value = ${(businesstripsandillnessadd.projectId != null) ? businesstripsandillnessadd.projectId : 0};
            }
        }

    </script>
</head>
<body>
<h1><div id="headerName"></div></h1>
    <br/>
    <form:form method="post" id="mainForm" commandName="businesstripsandillnessadd" name="mainForm" cssClass="chooseform">

        <div class="lowspace checkboxeslabel">
            Сотрудник:
        </div>

        <div class="lowspace checkboxesselect">
            <c:choose>
                <c:when test="${reportId == null}">
                    <form:select path="employeeId" id="employeeId" onmouseover="tooltip.show(getTitle(this));"
                                 onmouseout="tooltip.hide();" required="true">
                        <form:options items="${employeeList}" itemLabel="name" itemValue="id"/>
                        <form:option value="-1" label="Выберите сотрудника"/>
                    </form:select>
                </c:when>
                <c:otherwise>
                    ${businesstripsandillnessadd.employee.name}
                </c:otherwise>
            </c:choose>
        </div>

        <c:choose>
            <c:when test="${reportId == null}">
                <div class="checkboxeslabel lowspace">Создать:</div>
                <div class="checkboxesselect lowspace">
                    <form:select path="reportType" id="reportType" onchange="updateView(this)"
                                 onmouseover="tooltip.show(getTitle(this));" onmouseout="tooltip.hide();" required="true">
                        <form:options items="${businesstripsandillnessadd.reportTypes}" itemLabel="name" itemValue="id" required="true" cssClass="date_picker"/>
                    </form:select>
                </div>
            </c:when>
            <c:when test="${reportId != null}">
                <form:hidden path="reportType" />
            </c:when>
        </c:choose>

        <div class="checkboxeslabel lowspace">Дата с:</div>
        <div class="checkboxesselect lowspace">
            <form:input path="beginDate" id="beginDate" class="date_picker" cssClass="fullwidth date_picker" data-dojo-type="DateTextBox" required="true"
                        onMouseOver="tooltip.show(getTitle(this));" onMouseOut="tooltip.hide();" onchange="updateProject()"/>
        </div>

        <div class="checkboxeslabel lowspace">Дата по:</div>
        <div class="checkboxesselect lowspace">
            <form:input path="endDate" id="endDate" class="date_picker" cssClass="fullwidth date_picker" data-dojo-type="DateTextBox" required="true"
                        onMouseOver="tooltip.show(getTitle(this));" onMouseOut="tooltip.hide();" onchange="updateProject()"/>
        </div>

        <div id="illness" class="creationform">

            <div class="checkboxeslabel lowspace">Основание:</div>
            <div class="checkboxesselect lowspace">
                <form:select path="reason" id="reason" onMouseOver="tooltip.show(getTitle(this));"
                             onMouseOut="tooltip.hide();" multiple="false" cssClass="date_picker">
                    <form:options items="${businesstripsandillnessadd.illnessTypes}" itemLabel="name" itemValue="id" required="true"/>
                </form:select>
            </div>

        </div>

        <div id="businesstrip" class="creationform">

            <div class="checkboxeslabel lowspace">Тип:</div>
            <div class="checkboxesselect lowspace">
                <form:select path="businessTripType" id="businessTripType" onMouseOver="tooltip.show(getTitle(this));"
                             onMouseOut="tooltip.hide();" multiple="false" required="true" onchange="updateProject()">
                    <form:options items="${businesstripsandillnessadd.businessTripTypes}" itemLabel="name" itemValue="id" required="true"/>
                </form:select>
            </div>

            <div id="businesstripproject">
                <div class="checkboxeslabel lowspace">Проект:</div>
                <div class="checkboxesselect lowspace">
                    <form:select path="projectId" id="projectId" onMouseOver="tooltip.show(getTitle(this));"
                                 onMouseOut="tooltip.hide();" multiple="false" />
                </div>
            </div>

        </div>

        <div class="checkboxeslabel lowspace">Комментарий:</div>
        <div class="comment lowspace">
            <form:textarea path="comment" id="comment" maxlength="600" rows="7" cssClass="fullwidth"/>
        </div>


        <div style="clear:both"/>

        <div class="bigspace onblock">
            <button id="create" type="button" class="button bigspace">Сохранить</button> <%--обработка в скрипте dojo.on--%>
            <button id="cancel" type="button" class="button bigspace" onclick="window.history.back()">Отмена</button>
        </div>

        <div id="errorboxdiv" name="errorboxdiv" class="off errorbox">
        </div>
        <div id="servervalidationerrorboxdiv" name="servervalidationerrorboxdiv" class="errorbox">
            <form:errors path="*" delimiter="<br/><br/>" />
        </div>

    </form:form>

</body>
</html>