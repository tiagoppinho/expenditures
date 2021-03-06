<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<% final String contextPath = request.getContextPath(); %>
<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
<script src='<%= contextPath + "/webjars/jquery-ui/1.11.1/jquery-ui.js" %>'></script>

<div class="page-header">
    <h2><spring:message code="missionsConfiguration.text.countryForNationalMissions"></spring:message></h2>
</div>

<div class="page-body">

    <form class="form-horizontal" action="<%= request.getContextPath() %>/missions/config/selectCountry" method="POST">
        ${csrf.field()}
        <div class="form-group">
            <label for="countryTerm" class="col-sm-2 control-label">
                <spring:message code="missionsConfiguration.label.country"/>
            </label>
            <div class="col-sm-10">
                <input id="countryTerm" type="text" class="form-control" required="required"/>
                <input type="hidden" id="country" name="countryCode" value="">
            </div>
        </div>
        <div class="form-group">
            <div class="col-sm-offset-2 col-sm-10">
                <button type="submit" class="btn btn-primary"><spring:message code="link.submit"/></button>
                <a href="<%= contextPath %>/missions/config" class="btn btn-default"><spring:message code="link.cancel"/></a>
            </div>
        </div>
    </form>

</div>



<script type="text/javascript">
    var contextPath = '<%= contextPath %>';

    $(document).ready(function() {
    });

    $(function() {
        $('#countryTerm').autocomplete({
            focus: function(event, ui) {
                //  $( "#searchString" ).val( ui.item.label);
                return false;
            },
            minLength: 2,   
            contentType: "application/json; charset=UTF-8",
            search  : function(){$(this).addClass('ui-autocomplete-loading');},
            open    : function(){$(this).removeClass('ui-autocomplete-loading');},
            source : function(request,response) {
                $.get(contextPath + "/missions/config/country/json", request,function(result) {
                    response($.map(result,function(item) {
                        return{
                            label: item.name,
                            value: item.code3
                        }
                    }));
                });
            },
            
            select: function( event, ui ) {
                $( "#countryTerm" ).val( ui.item.label );
                $( "#country" ).val( ui.item.value );               
                return false;
            }
        });
    });

</script>