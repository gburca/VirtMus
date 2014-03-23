<?xml version="1.0" encoding="UTF-8"?>
<!-- vi:ai:tabstop=4:shiftwidth=4:softtabstop=4
-->

<!--
    Document   : SongTransform.xsl
    Created on : April 11, 2009, 1:31 PM
    Author     : Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
    Description:
        Purpose of transformation is to convert version 2.0 of the *.song.xml file which
        contains a lot of "extras", such as mutexes, Synchronized collections, etc...,
        into the simple format:

        <song>
            <pages>
                <page/>
            </pages>
        </song>
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0">

<xsl:output method="xml" omit-xml-declaration="no" indent="yes" />


<xsl:template match="/song">
	<xsl:element name="song">
        <xsl:attribute name="version">
            <xsl:value-of select="@version"/>
        </xsl:attribute>
        <xsl:if test="name">
            <name><xsl:value-of select="name"/></name>
        </xsl:if>
        <xsl:if test="tags">
            <tags><xsl:value-of select="tags"/></tags>
        </xsl:if>
		<pages>
			<xsl:apply-templates select="//page"/>
		</pages>
	</xsl:element>
</xsl:template>


<xsl:template match="page">
	<page>
		<xsl:if test="name">
			<name><xsl:value-of select="name"/></name>
		</xsl:if>

		<xsl:apply-templates select="sourceFile"/>

		<xsl:apply-templates select="rotation"/>

		<xsl:apply-templates select="annotationSVG"/>

	</page>
</xsl:template>

<xsl:template match="annotationSVG">
    <xsl:copy-of select="@* | ."/>
</xsl:template>

<!--xsl:template match="annotationSVG">
    <xsl:element name="annotationSVG">
        <xsl:if test="@width">
            <xsl:attribute name="width">
                <xsl:value-of select="@width"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:if test="@height">
            <xsl:attribute name="height">
                <xsl:value-of select="@height"/>
            </xsl:attribute>
        </xsl:if>
        <xsl:value-of select="node()"/>
    </xsl:element>
</xsl:template-->

<xsl:template match="sourceFile">
    <xsl:element name="sourceFile">
        <!-- Transfer the pageNum attribute, if it exists, to the output. -->
        <xsl:if test="@pageNum">
            <xsl:attribute name="pageNum">
                <xsl:value-of select="@pageNum"/>
            </xsl:attribute>
        </xsl:if>

        <xsl:if test="@reference">
            <xsl:attribute name="reference">
                <xsl:value-of select="@reference"/>
            </xsl:attribute>
        </xsl:if>
		<xsl:choose>
			<xsl:when test="file">
				<xsl:value-of select=".//path"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="."/>
			</xsl:otherwise>
		</xsl:choose>
    </xsl:element>
</xsl:template>

<xsl:template match="rotation">
    <xsl:element name="rotation">
        <xsl:if test="@reference">
        <xsl:attribute name="reference">
            <xsl:value-of select="@reference"/>
        </xsl:attribute>
        </xsl:if>
        <xsl:value-of select="."/>
    </xsl:element>
</xsl:template>

<!-- An attempt to use variables for XPath -->

<!-- xsl:template match="/|*" mode="path">
   <xsl:param name="rule" select="/.." />
   <xsl:param name="path" select="substring($rule/expression, 2)" />
   <xsl:choose>
      <xsl:when test="contains($path, '/')">
         <xsl:apply-templates
               select="*[name() = substring-before($path, '/')]">
            <xsl:with-param name="path"
                            select="substring-after($path, '/')" />
            <xsl:with-param name="rule" select="$rule" />
         </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
         <xsl:apply-templates select="$rule">
            <xsl:with-param name="node"
                            select="*[name() = $path]" />
         </xsl:apply-templates>
      </xsl:otherwise>
   </xsl:choose>
</xsl:template -->

</xsl:stylesheet>
