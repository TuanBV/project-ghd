package com.example.mcprice.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class GenericPriceExtractorTest {

    private final GenericPriceExtractor extractor = new GenericPriceExtractor(new ObjectMapper());

    @Test
    void extract_jsonLdOfferWithDirectPrice_returnsPrice() {
        Document document = parse("""
                <script type="application/ld+json">
                {"@context":"https://schema.org","@type":"Product","name":"TV",
                 "offers":{"@type":"Offer","price":"10690000","priceCurrency":"VND","availability":"https://schema.org/InStock"}}
                </script>
                """);

        var result = extractor.extract(document);

        assertThat(result).isPresent();
        assertThat(result.get().priceText()).isEqualTo("10690000");
        assertThat(result.get().availabilityText()).isEqualTo("https://schema.org/InStock");
    }

    @Test
    void extract_jsonLdOfferWithNestedPriceSpecification_returnsPrice() {
        // Dang thuong gap o WordPress/WooCommerce qua plugin Yoast SEO: gia nam trong
        // offers.priceSpecification.price thay vi offers.price truc tiep (vd dienmay88.vn).
        Document document = parse("""
                <script type="application/ld+json">
                {"@context":"https://schema.org","@graph":[{"@type":"Product","name":"May giat",
                 "offers":[{"@type":"Offer","priceSpecification":{"price":"25100000","priceCurrency":"VND","@type":"PriceSpecification"},
                 "availability":"http://schema.org/InStock"}]}]}
                </script>
                """);

        var result = extractor.extract(document);

        assertThat(result).isPresent();
        assertThat(result.get().priceText()).isEqualTo("25100000");
    }

    @Test
    void extract_jsonLdAggregateOfferWithLowPrice_fallsBackToLowPrice() {
        Document document = parse("""
                <script type="application/ld+json">
                {"@type":"Product","name":"TV","offers":{"@type":"AggregateOffer","lowPrice":"9990000","highPrice":"11990000"}}
                </script>
                """);

        var result = extractor.extract(document);

        assertThat(result).isPresent();
        assertThat(result.get().priceText()).isEqualTo("9990000");
    }

    @Test
    void extract_jsonLdWithoutProductType_fallsThroughToMetaTag() {
        Document document = parse("""
                <script type="application/ld+json">
                {"@context":"http://schema.org","@type":"BreadcrumbList","itemListElement":[]}
                </script>
                <meta property="og:price:amount" content="11,590,000₫">
                """);

        var result = extractor.extract(document);

        assertThat(result).isPresent();
        assertThat(result.get().priceText()).isEqualTo("11,590,000₫");
    }

    @Test
    void extract_noStructuredData_fallsBackToCommonCssClass() {
        Document document = parse("""
                <div class="product-price">1.234.000</div>
                """);

        var result = extractor.extract(document);

        assertThat(result).isPresent();
        assertThat(result.get().priceText()).isEqualTo("1.234.000");
    }

    @Test
    void extract_nothingFound_returnsEmpty() {
        Document document = parse("<div>Khong co gia o day</div>");

        assertThat(extractor.extract(document)).isEmpty();
    }

    private Document parse(String html) {
        return Jsoup.parse(html);
    }
}
