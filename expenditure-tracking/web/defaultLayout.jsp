<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<html:html xhtml="true">
<head>
	<tiles:insert attribute="head" ignore="true"/>
</head>

<body>
	<div id="container">

		<div id="header">
			<tiles:insert attribute="pageHeader" ignore="true"/>
		</div>

		<div id="tabs10">
  			<ul>
  				<tiles:insert attribute="topBar"/>
  			</ul>
		</div>

		<div id="container2">
			<div id="sidebar">
				<tiles:insert attribute="sideBar"/>
			</div>

			<div id="content">
			  	<tiles:insert attribute="body" ignore="true"/>
			  	<br/>
			  	<br/>
			</div>

			<div id="footer">
				<tiles:insert attribute="footer"/>
			</div>
		</div>

	</div>
</body>
</html:html>
