export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  username: string
  role: string
  expiresInSeconds: number
}

export interface ProductSummary {
  id: number
  skuOriginal: string | null
  title: string
  productUrl: string | null
  brand: string | null
  googleCategory: string | null
  availability: string
  currentWebsitePrice: number | null
  currentMcPrice: number | null
  currency: string
  validCompetitorSourceCount: number
  averageCompetitorPrice: number | null
  suggestedPrice: number | null
  recommendationStatus: string | null
  hasMatchConflict: boolean
}

export interface AliasDto {
  id: number
  aliasType: string
  aliasOriginal: string
  aliasNormalized: string
  confirmed: boolean
  confidence: number
}

export interface CompetitorListingDto {
  id: number
  productId: number
  productTitle: string | null
  productSku: string | null
  competitorId: number
  competitorName: string
  url: string
  externalSku: string | null
  matchMethod: string
  matchScore: number
  matchReason: string | null
  matchStatus: string
  active: boolean
}

export interface PriceRecommendationSource {
  observationId: number
  competitorId: number
  competitorName: string
  price: number | null
  capturedAt: string
  included: boolean
  reason: string
}

export interface PriceRecommendationDto {
  id: number
  productId: number
  productTitle: string
  currentPrice: number | null
  rawAveragePrice: number | null
  roundedPrice: number | null
  finalSuggestedPrice: number | null
  includedSourceCount: number
  excludedSourceCount: number
  status: string
  overridePrice: number | null
  overrideBy: string | null
  overrideReason: string | null
  overrideExpiresAt: string | null
  approvedBy: string | null
  approvedAt: string | null
  createdAt: string
  sources: PriceRecommendationSource[]
}

export interface ProductDetail {
  id: number
  mcOfferId: string | null
  skuOriginal: string | null
  skuNormalized: string | null
  title: string
  description: string | null
  productUrl: string | null
  imageUrl: string | null
  brand: string | null
  googleCategory: string | null
  productType: string | null
  condition: string
  availability: string
  currentWebsitePrice: number | null
  currentMcPrice: number | null
  currency: string
  active: boolean
  aliases: AliasDto[]
  competitorListings: CompetitorListingDto[]
  latestRecommendation: PriceRecommendationDto | null
}

export interface CompetitorDto {
  id: number
  name: string
  baseUrl: string
  enabled: boolean
  crawlMode: string
  requestsPerMinute: number
  timeoutSeconds: number
  extractorConfig: Record<string, unknown>
  lastSuccessAt: string | null
  lastErrorAt: string | null
  lastErrorMessage: string | null
  lastDiscoveryJobRunId: number | null
  lastDiscoveryStatus: string | null
  lastDiscoveryProgressPercent: number | null
  /** Tong so trang san pham thuc tren site doi thu (khop SKU + da xac minh qua HTML nhung khong trung SKU nao). */
  discoveredUrlCount: number
  /** Trong so discoveredUrlCount, so luong khop duoc voi 1 san pham cu the trong DB cua ban. */
  matchedProductCount: number
}

export interface ImportRunDto {
  id: number
  importType: string
  fileName: string | null
  status: string
  totalRows: number
  successRows: number
  issueRows: number
  startedAt: string | null
  finishedAt: string | null
}

export interface ImportIssueDto {
  id: number
  importRowId: number | null
  rowNumber: number | null
  issueType: string
  severity: string
  message: string
  resolved: boolean
}

export interface JobRunDto {
  id: number
  jobKey: string
  triggerType: string
  status: string
  totalItems: number
  successItems: number
  failedItems: number
  progressPercent: number
  startedAt: string | null
  finishedAt: string | null
  errorDetail: string | null
  correlationId: string | null
}

export interface DashboardSummary {
  totalMcProducts: number
  matchedProducts: number
  unmatchedProducts: number
  conflictProducts: number
  productsBySourceCount: Record<string, number>
  recommendationsByStatus: Record<string, number>
  priceIncreasedCount: number
  priceDecreasedCount: number
  priceUnchangedCount: number
  totalPriceDifference: number
  averagePriceDifference: number
  crawlSuccessRateByCompetitor: Record<string, number>
  staleObservationCount: number
  lastJobRun: { jobKey: string; status: string; finishedAt: string | null } | null
  lastJobError: { jobKey: string; status: string; finishedAt: string | null } | null
  merchantSyncSuccessCount: number
  merchantSyncFailedCount: number
}

export interface PricePolicyDto {
  id: number
  scope: string
  category: string | null
  productId: number | null
  minimumCompetitorCount: number
  maxObservationAgeHours: number
  roundingStep: number
  maxIncreasePercent: number
  maxDecreasePercent: number
  outlierThresholdPercent: number
  outlierStrategy: string
  requireManualApproval: boolean
  minimumAllowedPrice: number | null
  maximumAllowedPrice: number | null
  autoPublishEnabled: boolean
}
