<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:template match="definition">
        <data>
            <xsl:apply-templates select="group"/>
        </data>
    </xsl:template>

    <xsl:template match="group">
        <item>
            <xsl:attribute name="value">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:attribute name="type">
                <xsl:value-of select="@type"/>
            </xsl:attribute>
            <xsl:apply-templates select="group"/>
            <xsl:apply-templates select="field"/>
        </item>
    </xsl:template>

    <xsl:template match="field">
        <item>
            <xsl:attribute name="value">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:attribute name="type">
                <xsl:value-of select="@type"/>
            </xsl:attribute>
        </item>
    </xsl:template>
</xsl:stylesheet>