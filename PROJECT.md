# MindLyric — Proje Dokümantasyonu

Bu dosya, projeyi yeni bir Claude oturumunda sıfırdan anlamak için hazırlanmıştır.
Tüm mimari, tamamlanan aşamalar, dosya yapısı ve geliştirme kuralları burada açıklanmıştır.

---

## Proje Nedir?

MindLyric, kullanıcıların günlük tutabileceği, ruh hallerini seçebileceği ve Claude AI tarafından
duygu analizi + kişisel öneriler alabileceği bir Android uygulamasıdır.
Bir ödev projesidir — kod sade, Türkçe yorumlu ve öğrenci seviyesinde yazılmalıdır.

---

## Teknoloji Yığını

- **Dil:** Kotlin
- **Mimari:** MVVM (Model-View-ViewModel)
- **Veritabanı:** Room (KSP ile)
- **Async:** Coroutines + StateFlow / LiveData
- **HTTP:** OkHttp (Claude API için)
- **Grafik:** MPAndroidChart (JitPack)
- **UI:** XML layout, Material Components, findViewById (ViewBinding yok)
- **minSdk:** 24 | **targetSdk:** 36 | **compileSdk:** 36

---

## Geliştirme Kuralları (ÖNEMLİ)

- Tüm kodlar **Türkçe yorum** içermelidir
- Kod sade ve açıklanmış olmalıdır — over-engineering yasak
- Yeni özellik = yeni `feature/xxx` branch → develop'a merge → branch sil
- Ana branch: `main` | Geliştirme branch: `develop`
- ViewBinding kullanılmıyor, `findViewById` kullanılıyor
- DB versiyonu her şema değişikliğinde artırılmalı, `fallbackToDestructiveMigration` aktif

---

## Paket Yapısı

```
com.mindlyric.mindlyric0
│
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── JournalDao.kt       — günlük DB sorguları
│   │   │   └── UserDao.kt          — kullanıcı DB sorguları
│   │   ├── db/
│   │   │   ├── MindLyricDatabase.kt — Room DB tanımı (şu an v5)
│   │   │   └── DbProvider.kt        — singleton DB erişimi
│   │   ├── entity/
│   │   │   ├── JournalEntryEntity.kt — günlük tablosu
│   │   │   └── UserEntity.kt         — kullanıcı tablosu
│   │   └── util/
│   │       └── HashUtil.kt           — SHA-256 şifre hash
│   ├── remote/
│   │   └── ClaudeService.kt          — Claude API HTTP çağrıları
│   └── repository/
│       ├── JournalRepository.kt      — günlük iş mantığı
│       └── UserRepository.kt         — kullanıcı iş mantığı + sealed class'lar
│
└── ui/
    ├── auth/
    │   ├── LoginActivity.kt
    │   ├── RegisterActivity.kt
    │   ├── AuthViewModel.kt
    │   └── AuthViewModelFactory.kt
    ├── journal/
    │   ├── JournalViewModel.kt
    │   ├── JournalViewModelFactory.kt
    │   └── JournalAdapter.kt
    ├── profile/
    │   ├── ProfileActivity.kt
    │   ├── ProfileViewModel.kt
    │   └── ProfileViewModelFactory.kt
    └── MainActivity.kt
```

---

## Veritabanı Şeması (Room v5)

### `users` tablosu — UserEntity
```
id          Long  PrimaryKey autoGenerate
username    String
email       String  (unique index)
password    String  (SHA-256 hash — düz metin asla saklanmaz)
avatarResId Int?    (R.drawable.avatar_X değeri, null ise avatar seçilmemiş)
```

### `journal_entries` tablosu — JournalEntryEntity
```
id               Long    PrimaryKey autoGenerate
userId           Long    (users.id ile eşleşir)
createdAt        Long    (System.currentTimeMillis() — epoch ms)
text             String  (günlük metni)
moodLabel        String? (kullanıcının seçtiği ruh hali, ör: "😊 Mutlu")
sentimentScore   Float?  (Claude API skoru, 1.0-10.0, null = henüz analiz edilmedi)
recommendation   String? (Claude'un kişisel öneri mesajı, null = henüz üretilmedi)
```

---

## Oturum Yönetimi

- **SharedPreferences** adı: `"mindlyric_prefs"`
- **Anahtar:** `"logged_in_user_id"` → `Long` olarak saklanır
- `LoginActivity.PREFS_NAME` ve `LoginActivity.KEY_USER_ID` sabitleri kullanılır
- Uygulama açıldığında `savedUserId` kontrol edilir; varsa direkt `MainActivity`'e geçilir
- "Beni Hatırla" checkbox ile çalışır; işaretliyse userId kaydedilir

---

## Auth Akışı

```
Register → email kontrolü → SHA-256 hash → DB insert → userId al → SharedPrefs'e yaz → MainActivity
Login    → email var mı? → şifre hash karşılaştır → userId al → SharedPrefs'e yaz → MainActivity
```

### LoginResult sealed class:
- `Success(userId: Long)` — giriş başarılı
- `UserNotFound` — bu email kayıtlı değil → "Bu e-posta ile kayıtlı bir hesap bulunamadı"
- `WrongPassword` — email var ama şifre yanlış → "Şifre hatalı"

---

## Claude API Entegrasyonu

### Güvenlik
- API anahtarı `local.properties` dosyasında saklanır (git'e commit edilmez)
- `local.properties`: `CLAUDE_API_KEY=sk-ant-api03-...`
- `app/build.gradle.kts` içinde `Properties()` ile okunur → `BuildConfig.CLAUDE_API_KEY`

### ClaudeService.kt — 3 fonksiyon:

**1. `analyzeSentiment(text: String): Float?`**
- Türkçe günlük metnini analiz eder
- 1-10 arası tam sayı döner (1-3 negatif, 4-6 nötr, 7-10 pozitif)
- Hata/timeout → null döner, uygulama çökmez

**2. `getRecommendation(text: String, score: Float): String?`**
- Metni ve skoru birlikte Claude'a gönderir
- Skora göre kişisel Türkçe mesaj üretir (2-3 cümle)
- Negatif → empati + cesaretlendirme, Nötr → olumlu bakış açısı, Pozitif → tebrik

**3. `getMoodPrediction(averageScore: Float, entryCount: Int): String?`**
- Son 7 günün ortalamasını Claude'a gönderir
- Trend yorumu + yarın için öngörü üretir

### API Çağrı Sırası (JournalViewModel — addEntry):
```
1. DB'ye yaz (sentimentScore=null, recommendation=null)  → kullanıcı hemen görür
2. analyzeSentiment(text) → Float? skor
3. updateSentimentScore(entryId, score)                   → kart güncellenir
4. getRecommendation(text, score) → String? öneri
5. updateRecommendation(entryId, recommendation)          → kart güncellenir
```

---

## Profil Sayfası

### Özellikler:
- **Avatar seçimi:** 20 adet `avatar_1.jpg` ... `avatar_20.jpg` (`res/drawable/` içinde)
- **İstatistikler:** Toplam günlük sayısı + en sık kullanılan ruh hali
- **Şifre değiştirme:** Mevcut şifre doğrulandıktan sonra yeni şifre hash'lenerek kaydedilir
- **Hesap silme:** Önce `journal_entries`, sonra `users` tablosundan silme
- **Trend grafiği:** Son 7 günün duygu skorları — MPAndroidChart BarChart
  - Kırmızı (#EF5350) = 1-3, Turuncu (#FFA726) = 4-6, Yeşil (#66BB6A) = 7-10
- **Yarın tahmini:** En az 2 skorlu günlük varsa "🔮 Yarın İçin Tahmin" kartı gösterilir

### ProfileState sealed class:
- `Idle` — beklemede
- `Loading` — işlem devam ediyor
- `PasswordChanged` — şifre değiştirildi
- `AccountDeleted` — hesap silindi → Login'e yönlendir
- `Error(message)` — hata mesajı

---

## Günlük Ekranı (MainActivity)

- RecyclerView + JournalAdapter + ListAdapter (DiffUtil)
- Ruh hali seçimi: HorizontalScrollView içinde chip'ler (Mutlu, Üzgün, Nötr, Sinirli, Yorgun)
- Uzun basma → silme onay dialogu
- Sağ üst köşe: profil avatar ikonu → ProfileActivity'e geçiş
- Her kart gösterir:
  - Tarih + ruh hali etiketi
  - Günlük metni
  - Duygu skoru (ProgressBar + emoji + X/10) — skor geldikten sonra görünür
  - Claude öneri mesajı (mor kutucuk) — öneri geldikten sonra görünür

---

## Layout Dosyaları

```
res/layout/
├── activity_login.xml          — giriş ekranı
├── activity_register.xml       — kayıt ekranı
├── activity_main.xml           — ana günlük ekranı
├── activity_profile.xml        — profil ekranı (grafik + tahmin + butonlar)
├── item_journal_entry.xml      — günlük kartı (skor + öneri dahil)
├── item_avatar.xml             — avatar seçim grid'indeki tek görsel
├── dialog_avatar_picker.xml    — avatar seçim dialogu (GridLayout)
└── dialog_change_password.xml  — şifre değiştirme dialogu

res/drawable/
├── bg_avatar_circle.xml        — profil avatar oval arka planı
├── bg_avatar_item.xml          — avatar seçim grid item arka planı
├── bg_recommendation.xml       — öneri kutucuğu hafif mor arka planı
├── avatar_1.jpg ... avatar_20.jpg — kullanıcı avatarları
```

---

## Bağımlılıklar (app/build.gradle.kts)

```kotlin
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.appcompat)
implementation(libs.material)
implementation(libs.androidx.activity)
implementation(libs.androidx.constraintlayout)
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
implementation(libs.lifecycle.runtime.ktx)
implementation(libs.lifecycle.viewmodel.ktx)
implementation(libs.okhttp)                                    // Claude API HTTP
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")   // Trend grafiği
```

`settings.gradle.kts` içinde JitPack repo'su eklenmiştir:
```kotlin
maven { url = uri("https://jitpack.io") }
```

---

## Tamamlanan Aşamalar

| Aşama | Branch | Açıklama |
|-------|--------|----------|
| 1 | feature/login-register-room | Kullanıcıya özel günlükler (userId) |
| 2 | feature/login-register-room | Profil sayfası, avatar, şifre, hesap silme |
| 3 | feature/sentiment-analysis | Claude API duygu analizi (1-10 skor) |
| 4 | feature/mood-trend-chart | Son 7 gün renkli çubuk grafik |
| 5 | feature/recommendation-system | Kişiselleştirilmiş Claude önerisi |
| 6 | feature/mood-prediction | Trend bazlı yarın tahmini |

---

## Araştırma Soruları & Hipotez Bağlantıları

### AS1 — Gelecek tahmini
> "AI, geçmiş duygu örüntülerini kullanarak gelecekteki ruh hali değişimlerini tahmin edebilir mi?"

**Karşılayan aşamalar: 3, 4, 6**
- Aşama 3: Her günlük Claude tarafından 1-10 skorlanıyor → geçmiş örüntü verisi birikir
- Aşama 4: Grafik ile kullanıcı kendi trendini görsel olarak fark edebilir
- Aşama 6: Son 7 günün ortalamasına dayanarak Claude yarın tahmini üretiyor

### AS2 — Zaman içinde görselleştirme
> "Duygu sınıflandırması, ruh hali değişimlerini zaman içinde güvenilir biçimde ortaya koyabilir mi?"

**Karşılayan aşamalar: 3, 4**
- Aşama 3: Tutarlı 1-10 skalası — her metin aynı standartla değerlendiriliyor
- Aşama 4: Son 7 günün renk kodlu çubuk grafiği, değişimi zaman ekseninde net gösteriyor

### AS3 — Kişisel öneri & iyilik hali
> "Kişiselleştirilmiş öneriler kullanıcıların iyilik halini iyileştirir mi?"

**Karşılayan aşama: 5**
- Her günlük sonrası metin + skor birlikte Claude'a gönderiliyor
- Kişiye özel Türkçe mesaj üretiliyor (empati / motivasyon / tebrik)
- Öneri günlük kartında görünür halde kullanıcıya sunuluyor

---

## Kurulum Adımları (Yeni Bilgisayar)

1. Projeyi klonla: `git clone <repo-url>` → `develop` branch'ine geç
2. Android Studio ile aç, Gradle sync yap
3. `local.properties` dosyasına Claude API anahtarını ekle:
   ```
   CLAUDE_API_KEY=sk-ant-api03-...
   ```
4. Uygulamayı ilk kez kurmadan önce cihazda varsa sil (Room DB versiyon uyuşmazlığı)
5. Run

> **Not:** `local.properties` git'e commit edilmez. Her yeni bilgisayarda bu adım tekrarlanmalıdır.

---

## Bilinen Davranışlar

- Skor ve öneri, günlük kaydedildikten **birkaç saniye sonra** gelir (API async çalışır)
- Eski günlüklerin skoru yoksa skor alanı gizlenir
- Trend grafiği için en az 1, tahmin için en az 2 skorlu günlük gerekir
- DB versiyonu değişince uygulama silinip yeniden kurulmalıdır (`fallbackToDestructiveMigration`)
- Heap sorunu yaşanırsa: Android Studio JDK kullan → `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`
