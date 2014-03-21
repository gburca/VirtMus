<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : PlayListTransform.xsl
    Created on : April 27, 2009, 10:22 PM
    Author     : gburca
    Description:
        For easy validation, we want to make sure the sequence is:
            <Name/>
            <SongFiles/>
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:output method="xml" omit-xml-declaration="no" indent="yes"/>

    <xsl:template match="/PlayList">
        <xsl:element name="PlayList">
            <xsl:attribute name="version">
                <xsl:value-of select="@version"/>
            </xsl:attribute>

            <xsl:if test="Name">
                <Name><xsl:value-of select="Name"/></Name>
            </xsl:if>
            
            <xsl:if test="Tags">
                <Tags><xsl:value-of select="Tags"/></Tags>
            </xsl:if>

            <xsl:if test="SongFiles">
                <SongFiles>
                    <xsl:apply-templates select="SongFiles/file"/>
                </SongFiles>
            </xsl:if>
        </xsl:element>
    </xsl:template>

    <xsl:template match="file">
        <file><xsl:value-of select="."/></file>
    </xsl:template>

</xsl:stylesheet>
