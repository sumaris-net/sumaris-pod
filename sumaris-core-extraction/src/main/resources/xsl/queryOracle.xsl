<?xml version="1.0" encoding="ISO-8859-15"?>
<!--
  #%L
  SUMARiS:: Core Extraction
  %%
  Copyright (C) 2018 - 2021 SUMARiS Consortium
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  
  You should have received a copy of the GNU General Public
  License along with this program.  If not, see
  <http://www.gnu.org/licenses/gpl-3.0.html>.
  #L%
  -->


<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text"/>

  <!-- ================ -->
  <!-- Global variables -->
  <!-- ================ -->
  <!-- carrier return -->
  <xsl:variable name="cr">
		<xsl:text>
</xsl:text>
  </xsl:variable>

  <!-- tabulation -->
  <xsl:variable name="tab">
    <xsl:text>    </xsl:text>
  </xsl:variable>

  <!-- ==== -->
  <!-- ROOT	-->
  <!-- ==== -->
  <xsl:template match="/queries">
    <xsl:apply-templates select="query"/>
  </xsl:template>
  <xsl:template match="/indexes">
    <xsl:apply-templates select="index"/>
  </xsl:template>

  <!-- ===== -->
  <!-- QUERY -->
  <!-- ===== -->
  <xsl:template match="query">
    <xsl:param name="prefix" select="$cr"/>
    <xsl:param name="childPrefix" select="concat($prefix, $tab)"/>

    <!-- DROP -->
    <xsl:if test="./@type = 'drop'">
      <xsl:text>DROP TABLE </xsl:text><xsl:value-of select="./@table"/>
    </xsl:if>

    <!-- DELETE -->
    <xsl:if test="./@type = 'delete'">
      <xsl:text>DELETE FROM </xsl:text><xsl:value-of select="./@table"/>

      <!-- WHERE -->
      <xsl:if test="./*=where">
        <xsl:value-of select="$prefix"/>
        <xsl:text>WHERE </xsl:text>
        <xsl:apply-templates select="where">
          <xsl:with-param name="prefix" select="$childPrefix"/>
        </xsl:apply-templates>
      </xsl:if>
    </xsl:if>

    <!-- CREATE -->
    <xsl:if test="./@type = 'create'">
      <xsl:choose>
        <xsl:when test="./@temp = 'true'">
          <xsl:text>DECLARE LOCAL TEMPORARY TABLE </xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>CREATE TABLE </xsl:text>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:value-of select="./@table"/>
      <xsl:text> AS </xsl:text>
    </xsl:if>

    <xsl:if test="not(./@type) or (./@type != 'drop' and ./@type != 'delete')">

      <!-- WITH -->
      <xsl:apply-templates select="with">
        <xsl:with-param name="prefix" select="$prefix"/>
      </xsl:apply-templates>

      <!-- SELECT -->
      <xsl:value-of select="$prefix"/>
      <xsl:text>SELECT </xsl:text>
      <xsl:value-of select="./@option"/><xsl:text> </xsl:text>
      <xsl:apply-templates select="select">
        <xsl:with-param name="prefix" select="$childPrefix"/>
      </xsl:apply-templates>

      <!-- CURSOR -->
      <xsl:if test="./*=nested_select">
        <xsl:apply-templates select="nested_select">
          <xsl:with-param name="prefix" select="$childPrefix"/>
        </xsl:apply-templates>
      </xsl:if>

      <!-- FROM -->
      <xsl:value-of select="$prefix"/>
      <xsl:text>FROM </xsl:text>
      <xsl:apply-templates select="from">
        <xsl:with-param name="prefix" select="$childPrefix"/>
      </xsl:apply-templates>

      <!-- WHERE -->
      <xsl:if test="./*=where">
        <xsl:value-of select="$prefix"/>
        <xsl:text>WHERE </xsl:text>
        <xsl:apply-templates select="where">
          <xsl:with-param name="prefix" select="$childPrefix"/>
        </xsl:apply-templates>
      </xsl:if>

      <!-- COMPLEMENT -->
      <xsl:if test="./*=complement">
        <xsl:apply-templates select="complement">
          <xsl:with-param name="prefix" select="$childPrefix"/>
        </xsl:apply-templates>
      </xsl:if>

      <!-- UNION -->
      <xsl:if test="./*=union">
        <xsl:apply-templates select="union">
          <xsl:with-param name="prefix" select="$childPrefix"/>
        </xsl:apply-templates>
      </xsl:if>

      <!-- UNION ALL-->
      <xsl:if test="./*=unionAll">
        <xsl:apply-templates select="unionAll">
          <xsl:with-param name="prefix" select="$childPrefix"/>
        </xsl:apply-templates>
      </xsl:if>

      <!-- GROUP BY -->
      <xsl:if test="./*=groupby">
        <xsl:value-of select="$prefix"/>
        <xsl:text>GROUP BY </xsl:text>
        <xsl:apply-templates select="groupby">
          <xsl:with-param name="prefix" select="$childPrefix"/>
        </xsl:apply-templates>
      </xsl:if>

      <!-- ORDER BY -->
      <xsl:if test="./*=orderby">
        <xsl:value-of select="$prefix"/>
        <xsl:text>ORDER BY </xsl:text>
        <xsl:apply-templates select="orderby">
          <xsl:with-param name="prefix" select="$childPrefix"/>
        </xsl:apply-templates>
      </xsl:if>

    </xsl:if>

    <!-- CREATE -->
    <xsl:if test="./@type = 'create'">
      <xsl:text> </xsl:text>
    </xsl:if>

  </xsl:template>


  <!-- ========= -->
  <!-- SUB QUERY -->
  <!-- ========= -->
  <xsl:template match="subquery">
    <xsl:param name="prefix" select="$cr"/>
    <xsl:param name="childPrefix" select="concat($prefix, $tab)"/>

    <!-- SELECT -->
    <xsl:value-of select="$prefix"/>
    <xsl:text>SELECT </xsl:text>
    <xsl:value-of select="./@option"/><xsl:text> </xsl:text>
    <xsl:apply-templates select="subselect">
      <xsl:with-param name="prefix" select="$childPrefix"/>
    </xsl:apply-templates>

    <!-- FROM -->
    <xsl:value-of select="$prefix"/>
    <xsl:text>FROM </xsl:text>
    <xsl:apply-templates select="from">
      <xsl:with-param name="prefix" select="$childPrefix"/>
    </xsl:apply-templates>

    <!-- WHERE -->
    <xsl:if test="./*=where">
      <xsl:value-of select="$prefix"/>
      <xsl:text>WHERE </xsl:text>
      <xsl:apply-templates select="where">
        <xsl:with-param name="prefix" select="$childPrefix"/>
      </xsl:apply-templates>
    </xsl:if>

    <!-- COMPLEMENT -->
    <xsl:if test="./*=complement">
      <xsl:apply-templates select="complement">
        <xsl:with-param name="prefix" select="$childPrefix"/>
      </xsl:apply-templates>
    </xsl:if>

    <!-- UNION -->
    <xsl:if test="./*=union">
      <xsl:apply-templates select="union">
        <xsl:with-param name="prefix" select="$childPrefix"/>
      </xsl:apply-templates>
    </xsl:if>

    <!-- GROUP BY -->
    <xsl:if test="./*=groupby">
      <xsl:value-of select="$prefix"/>
      <xsl:text>GROUP BY </xsl:text>
      <xsl:apply-templates select="groupby">
        <xsl:with-param name="prefix" select="$childPrefix"/>
      </xsl:apply-templates>
    </xsl:if>

    <!-- ORDER BY -->
    <xsl:if test="./*=orderby">
      <xsl:value-of select="$prefix"/>
      <xsl:text>ORDER BY </xsl:text>
      <xsl:apply-templates select="orderby">
        <xsl:with-param name="prefix" select="$childPrefix"/>
      </xsl:apply-templates>
    </xsl:if>
  </xsl:template>


  <!-- ==== -->
  <!-- WITH -->
  <!-- ==== -->
  <xsl:template match="with">
    <xsl:param name="prefix"/>
    <xsl:value-of select="$prefix"/>

    <!-- add with keyword on first call -->
    <xsl:if test="position() = 1">
      <xsl:text>WITH</xsl:text>
    </xsl:if>
    <xsl:text> </xsl:text>

    <!-- add alias -->
    <xsl:value-of select="./@alias" disable-output-escaping="yes"/>
    <!-- open parenthesis -->
    <xsl:text> as (</xsl:text>

    <!-- apply templates inside with -->
    <xsl:apply-templates select="*"/>

    <!-- close parenthesis -->
    <xsl:text>)</xsl:text>

    <!-- add a comma on non-last block -->
    <xsl:if test="position() != last()">
      <xsl:text>, </xsl:text>
    </xsl:if>
  </xsl:template>


  <!-- ====== -->
  <!-- SELECT -->
  <!-- ====== -->
  <xsl:template match="select">
    <xsl:param name="prefix"/>
    <xsl:value-of select="$prefix"/>
    <xsl:call-template name="addTagContent">
      <xsl:with-param name="prefix" select="$prefix"/>
    </xsl:call-template>
    <xsl:text> </xsl:text>
    <xsl:if test="./@alias != ''">
      <xsl:text>"</xsl:text>
      <xsl:value-of select="./@alias" disable-output-escaping="yes"/>
      <xsl:text>"</xsl:text>
    </xsl:if>
    <xsl:if test="position() != last()">
      <xsl:text>, </xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- ========== -->
  <!-- SUB SELECT -->
  <!-- ========== -->
  <xsl:template match="subselect">
    <xsl:param name="prefix"/>
    <xsl:value-of select="$prefix"/>
    <xsl:call-template name="addTagContent">
      <xsl:with-param name="prefix" select="$prefix"/>
    </xsl:call-template>
    <xsl:text> </xsl:text>
    <xsl:if test="./@alias != ''">
      <xsl:text>"</xsl:text>
      <xsl:value-of select="./@alias" disable-output-escaping="yes"/>
      <xsl:text>"</xsl:text>
    </xsl:if>
    <xsl:if test="position() != last()">
      <xsl:text>, </xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- ==== -->
  <!-- FROM -->
  <!-- ==== -->
  <xsl:template match="from">
    <xsl:param name="prefix"/>
    <xsl:value-of select="$prefix"/>
    <xsl:if test="position() != 1 and not(./@join)">
      <xsl:text>, </xsl:text>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="./*=subquery">
        <xsl:text>(</xsl:text>
        <xsl:apply-templates select="subquery">
          <xsl:with-param name="prefix" select="concat($prefix, $tab)"/>
        </xsl:apply-templates>
        <xsl:value-of select="$prefix"/>
        <xsl:text>)</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="addTagContent">
          <xsl:with-param name="prefix" select="$prefix"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text> </xsl:text>
    <xsl:if test="./@alias != ''">
      <xsl:value-of select="./@alias" disable-output-escaping="yes"/>
      <xsl:text> </xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- ===== -->
  <!-- WHERE -->
  <!-- ===== -->
  <xsl:template match="where">
    <xsl:param name="prefix"/>
    <xsl:value-of select="$prefix"/>
    <xsl:if test="./@operator != ''">
      <xsl:value-of select="./@operator" disable-output-escaping="yes"/>
      <xsl:text> </xsl:text>
    </xsl:if>
    <xsl:choose>
      <!-- SUB QUERY -->
      <!-- IN -->
      <xsl:when test="./*=in">
        <xsl:apply-templates select="in">
          <xsl:with-param name="prefix" select="$prefix"/>
        </xsl:apply-templates>
      </xsl:when>
      <!-- NOT IN -->
      <xsl:when test="./*=notin">
        <xsl:apply-templates select="notin">
          <xsl:with-param name="prefix" select="$prefix"/>
        </xsl:apply-templates>
      </xsl:when>
      <!-- NESTED_WHERE -->
      <xsl:when test="./*=where">
        <xsl:text>(</xsl:text>
        <xsl:apply-templates select="where">
          <xsl:with-param name="prefix" select="concat($prefix, $tab)"/>
        </xsl:apply-templates>
        <xsl:value-of select="$prefix"/>
        <xsl:text>)</xsl:text>
      </xsl:when>
      <!-- CDATA -->
      <xsl:otherwise>
        <xsl:call-template name="addTagContent">
          <xsl:with-param name="prefix" select="$prefix"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ======== -->
  <!-- ORDER BY -->
  <!-- ======== -->
  <xsl:template match="orderby">
    <xsl:param name="prefix"/>
    <xsl:value-of select="$prefix"/>
    <xsl:call-template name="addTagContent">
      <xsl:with-param name="prefix" select="$prefix"/>
    </xsl:call-template>
    <xsl:if test="./@direction!= ''">
      <xsl:text> </xsl:text>
      <xsl:value-of select="./@direction" disable-output-escaping="yes"/>
    </xsl:if>
    <xsl:if test="position() != last()">
      <xsl:text>, </xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- ======== -->
  <!-- GROUP BY -->
  <!-- ======== -->
  <xsl:template match="groupby">
    <xsl:param name="prefix"/>
    <xsl:value-of select="$prefix"/>
    <xsl:call-template name="addTagContent">
      <xsl:with-param name="prefix" select="$prefix"/>
    </xsl:call-template>
    <xsl:if test="position() != last()">
      <xsl:text>, </xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- ========== -->
  <!-- COMPLEMENT -->
  <!-- ========== -->
  <xsl:template match="complement">
    <xsl:param name="prefix"/>
    <xsl:value-of select="$prefix"/>
    <xsl:call-template name="addTagContent">
      <xsl:with-param name="prefix" select="$prefix"/>
    </xsl:call-template>
  </xsl:template>

  <!-- == -->
  <!-- IN -->
  <!-- == -->
  <xsl:template match="in">
    <xsl:param name="prefix"/>
    <xsl:choose>
      <!-- SUB QUERY -->
      <xsl:when test="./*=subquery">
        <xsl:value-of select="./@field" disable-output-escaping="yes"/>
        <xsl:text> IN </xsl:text>
        <xsl:value-of select="$prefix"/>
        <xsl:text>(</xsl:text>
        <xsl:apply-templates select="subquery">
          <xsl:with-param name="prefix" select="concat($prefix, $tab)"/>
        </xsl:apply-templates>
        <xsl:value-of select="$prefix"/>
        <xsl:text>)</xsl:text>
      </xsl:when>
      <!-- CDATA -->
      <xsl:otherwise>
        <xsl:text>(</xsl:text>
        <xsl:value-of select="./@field" disable-output-escaping="yes"/>
        <xsl:text> IN </xsl:text>
        <xsl:call-template name="addTagContent">
          <xsl:with-param name="prefix" select="$prefix"/>
        </xsl:call-template>
        <xsl:text>)</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ====== -->
  <!-- NOT IN -->
  <!-- ====== -->
  <xsl:template match="notin">
    <xsl:param name="prefix"/>
    <xsl:choose>
      <!-- SUB QUERY -->
      <xsl:when test="./*=subquery">
        <xsl:value-of select="./@field" disable-output-escaping="yes"/>
        <xsl:text> NOT IN </xsl:text>
        <xsl:value-of select="$prefix"/>
        <xsl:text>(</xsl:text>
        <xsl:apply-templates select="subquery">
          <xsl:with-param name="prefix" select="concat($prefix, $tab)"/>
        </xsl:apply-templates>
        <xsl:value-of select="$prefix"/>
        <xsl:text>)</xsl:text>
      </xsl:when>
      <!-- CDATA -->
      <xsl:otherwise>
        <xsl:text>(</xsl:text>
        <xsl:value-of select="./@field" disable-output-escaping="yes"/>
        <xsl:text> NOT IN </xsl:text>
        <xsl:call-template name="addTagContent">
          <xsl:with-param name="prefix" select="$prefix"/>
        </xsl:call-template>
        <xsl:text>)</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ============= -->
  <!-- NESTED SELECT -->
  <!-- ============= -->
  <xsl:template match="nested_select">
    <xsl:param name="prefix"/>
    <xsl:value-of select="$prefix"/>
    <xsl:text>, CURSOR (</xsl:text>
    <xsl:apply-templates select="query">
      <xsl:with-param name="prefix" select="concat($prefix, $tab)"/>
    </xsl:apply-templates>
    <xsl:value-of select="$prefix"/>
    <xsl:text>) AS </xsl:text>
    <xsl:value-of select="./query/@name"/>
  </xsl:template>

  <!-- ===== -->
  <!-- UNION -->
  <!-- ===== -->
  <xsl:template match="union">
    <xsl:param name="prefix"/>
    <xsl:value-of select="$prefix"/>
    <xsl:text>UNION</xsl:text>
    <xsl:apply-templates select="subquery">
      <xsl:with-param name="prefix" select="concat($prefix, $tab)"/>
    </xsl:apply-templates>
  </xsl:template>


  <!-- ========= -->
  <!-- UNION ALL -->
  <!-- ========= -->
  <xsl:template match="unionAll">
    <xsl:param name="prefix"/>
    <xsl:value-of select="$prefix"/>
    <xsl:text>UNION ALL</xsl:text>
    <xsl:apply-templates select="subquery">
      <xsl:with-param name="prefix" select="concat($prefix, $tab)"/>
    </xsl:apply-templates>
  </xsl:template>

  <!-- =============== -->
  <!-- Add tag content -->
  <!-- =============== -->
  <xsl:template name="addTagContent">
    <xsl:param name="prefix"/>
    <xsl:param name="firstLine" select="true()"/>
    <xsl:param name="content" select="."/>
    <xsl:choose>
      <xsl:when test="contains($content, $cr)">
        <xsl:variable name="line" select="normalize-space(substring-before($content, $cr))"/>
        <xsl:choose>
          <xsl:when test="$line != ''">
            <xsl:if test="$firstLine = false()">
              <xsl:value-of select="$prefix"/>
            </xsl:if>
            <xsl:value-of select="$line" disable-output-escaping="yes"/>
            <xsl:call-template name="addTagContent">
              <xsl:with-param name="content" select="substring-after($content, $cr)"/>
              <xsl:with-param name="prefix" select="$prefix"/>
              <xsl:with-param name="firstLine" select="false()"/>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="addTagContent">
              <xsl:with-param name="content" select="substring-after($content, $cr)"/>
              <xsl:with-param name="prefix" select="$prefix"/>
              <xsl:with-param name="firstLine" select="$firstLine"/>
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="line" select="normalize-space($content)"/>
        <xsl:if test="$line != ''">
          <xsl:if test="$firstLine = false()">
            <xsl:value-of select="$prefix"/>
          </xsl:if>
          <xsl:value-of select="$line" disable-output-escaping="yes"/>
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ===== -->
  <!-- INDEX -->
  <!-- ===== -->
  <xsl:template match="index">

    <!-- CREATE -->
    <xsl:if test="./@type = 'create'">
      <xsl:text>CREATE INDEX </xsl:text>
      <xsl:value-of select="./@name"/>
      <xsl:text> ON </xsl:text>
      <xsl:value-of select="./@table"/>
      <xsl:text> (</xsl:text>
      <xsl:value-of select="./@field"/>
      <xsl:text>)</xsl:text>
    </xsl:if>

    <!-- DROP -->
    <xsl:if test="./@type = 'drop'">
      <xsl:text>DROP INDEX IF EXISTS </xsl:text>
      <xsl:value-of select="./@name"/>
    </xsl:if>

  </xsl:template>


</xsl:stylesheet>
