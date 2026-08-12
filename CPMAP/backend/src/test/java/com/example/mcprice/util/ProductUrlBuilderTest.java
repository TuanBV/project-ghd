package com.example.mcprice.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Cac gia tri ky vong duoc doi chieu truc tiep voi URL that dang chay tren tongkhodienmaymienbac.com
 * (fetch ngay 2026-08-12), khong phai tu suy dien — dam bao thuat toan slugify khop chinh xac voi
 * cach WordPress sinh permalink tren website that.
 */
class ProductUrlBuilderTest {

    @Test
    void buildOwnWebsiteUrl_simpleTitleNoDiacritics_mapsSpacesToSingleDashes() {
        assertThat(ProductUrlBuilder.buildOwnWebsiteUrl("Smart Tivi Samsung Neo QLED 4K 75 inch 75QN70F"))
                .isEqualTo("https://tongkhodienmaymienbac.com/smart-tivi-samsung-neo-qled-4k-75-inch-75qn70f/");
    }

    @Test
    void buildOwnWebsiteUrl_bracketsAreDeletedNotDashed() {
        assertThat(ProductUrlBuilder.buildOwnWebsiteUrl("Smart Tivi QLED Samsung 4K 65 inch QA65Q7F5A [65Q7F]"))
                .isEqualTo("https://tongkhodienmaymienbac.com/smart-tivi-qled-samsung-4k-65-inch-qa65q7f5a-65q7f/");
    }

    @Test
    void buildOwnWebsiteUrl_parenthesesAreDeletedNotDashed() {
        assertThat(ProductUrlBuilder.buildOwnWebsiteUrl("Tủ lạnh Toshiba Inverter 515 lít GR-RF665WIA-PGV(22)-XK"))
                .isEqualTo("https://tongkhodienmaymienbac.com/tu-lanh-toshiba-inverter-515-lit-gr-rf665wia-pgv22-xk/");
    }

    @Test
    void buildOwnWebsiteUrl_dotsBecomeDashes() {
        assertThat(ProductUrlBuilder.buildOwnWebsiteUrl("Điều hòa 1 chiều Midea MSMA1-12CR 12.000 BTU"))
                .isEqualTo("https://tongkhodienmaymienbac.com/dieu-hoa-1-chieu-midea-msma1-12cr-12-000-btu/");
    }

    @Test
    void buildOwnWebsiteUrl_existingHyphensInSkuArePreserved() {
        assertThat(ProductUrlBuilder.buildOwnWebsiteUrl("Máy giặt Sharp 10Kg ES-W100PV-H"))
                .isEqualTo("https://tongkhodienmaymienbac.com/may-giat-sharp-10kg-es-w100pv-h/");
    }

    @Test
    void buildOwnWebsiteUrl_vietnameseDiacriticsIncludingDStroke() {
        assertThat(ProductUrlBuilder.buildOwnWebsiteUrl("Tủ lạnh LG Inverter 574 lít LFB58BLMA"))
                .isEqualTo("https://tongkhodienmaymienbac.com/tu-lanh-lg-inverter-574-lit-lfb58blma/");
    }

    @Test
    void buildOwnWebsiteUrl_nullOrBlankTitle_returnsNull() {
        assertThat(ProductUrlBuilder.buildOwnWebsiteUrl(null)).isNull();
        assertThat(ProductUrlBuilder.buildOwnWebsiteUrl("   ")).isNull();
        assertThat(ProductUrlBuilder.buildOwnWebsiteUrl("")).isNull();
    }

    @Test
    void buildOwnWebsiteUrl_titleWithOnlyPunctuation_returnsNull() {
        assertThat(ProductUrlBuilder.buildOwnWebsiteUrl("()[]...")).isNull();
    }
}
