# 🤖 Neden Claude API Kullandık?

---

## 🔍 Başlangıçtaki Plan: Geleneksel Yapay Zeka Kütüphanesi

Projeye başlarken kullanıcıların günlük metinlerini analiz etmek için **yerel bir makine öğrenmesi kütüphanesi** (TensorFlow Lite veya ML Kit gibi) kullanmayı planladık.

Bu yaklaşımda şu akış söz konusuydu:

```
Kullanıcı günlük yazar
        ↓
Uygulama metni yerel bir ML modeline gönderir
        ↓
Model "olumlu / olumsuz / nötr" tahmini üretir
        ↓
Sonuç ekrana yansıtılır
```

---

## ❌ Geleneksel Yaklaşımın Sorunları

| Sorun | Açıklama |
|-------|----------|
| 📦 **Ağır model dosyaları** | TFLite modelleri onlarca MB yer kaplar, uygulama şişer |
| 🌐 **Türkçe desteği zayıf** | Hazır modellerin büyük çoğunluğu İngilizce metinler için eğitilmiş |
| 🔢 **Sadece skor** | "Olumlu / Olumsuz" etiketinin ötesine geçemiyor, kişisel mesaj üretemez |
| 🔧 **Bakım zorluğu** | Modeli güncel tutmak, yeniden eğitmek ciddi bir iş yükü gerektirir |
| 💬 **Tek yönlü analiz** | Tahmin, öneri, empati gibi bağlam gerektiren görevler yapılamaz |

---

## ✅ Neden Claude API'ye Geçtik?

Araştırmalarımız sonucunda **Anthropic Claude API**'nin projemizin ihtiyaçlarına çok daha uygun olduğunu gördük.

### Temel Fark:

> 🏚️ **Geleneksel ML:** "Bu metin %78 negatif"
>
> 🧠 **Claude API:** "Bugün zorlu bir gün geçirdiğini anlıyorum. Kendine iyi bak, yarın yeni bir gün."

Claude; bağlamı anlayan, Türkçe'yi akıcı konuşan ve sadece skor değil **anlamlı bir insan yanıtı** üretebilen bir model.

---

## 🛠️ Projede Claude API Nasıl Kullanıyoruz?

`ClaudeService.kt` dosyasında Claude API'ye **3 farklı amaç için** istek gönderiyoruz:

---

### 1️⃣ Duygu Skoru Analizi — `analyzeSentiment()`

```
Kullanıcı günlük yazar → Claude metni okur → 1–10 arası skor döner
```

**Örnek:**
- Kullanıcı: *"Bugün çok yoruldum, her şey ters gitti..."*
- Claude → `3` (negatif)

**Neden bu önemli?**
Günlüklerin zaman içindeki değişimi grafik olarak gösterilir. 1 sayıyı üretmek için büyük bir model kurmaya gerek yok — Claude bunu bağlamı anlayarak yapıyor.

---

### 2️⃣ Kişisel Öneri Mesajı — `getRecommendation()`

```
Günlük metni + duygu skoru → Claude değerlendirir → Kişisel Türkçe mesaj döner
```

**Örnekler:**
- Skor 2 ise → Empati kurar, cesaretlendirir
- Skor 5 ise → Nötr bakış açısı sunar
- Skor 9 ise → Tebrik eder, iyi enerjiyi pekiştirir

**Neden bu önemli?**
Kullanıcı sadece bir sayı görmez — kendi duygusuna özel, insani bir yanıt alır. Bu deneyimi tamamen farklı kılıyor.

---

### 3️⃣ Yarın Tahmini — `getMoodPrediction()`

```
Son 7 günün ortalama skoru → Claude örüntüyü analiz eder → Kısa gelecek tahmini döner
```

**Örnek:**
- Son 7 gün ortalaması: 4.2 / 10
- Claude → *"Son günlerde zor bir dönemden geçiyorsun. Bu dalgalanmalar normal; küçük adımlarla kendinle ilgilenmeye devam et, önümüzdeki günler daha aydınlık olacak."*

**Neden bu önemli?**
Kullanıcıya geleceğe dair umut veren, gerçekçi ve motive edici bir içgörü sunuluyor — bu bir ML sınıflandırıcısının yapabileceğinin çok ötesinde.

---

## 📊 Karşılaştırma: ML Kütüphanesi vs Claude API

| Özellik | ML Kütüphanesi | Claude API |
|---------|---------------|------------|
| Türkçe metin anlama | ⚠️ Sınırlı | ✅ Güçlü |
| Duygu skoru | ✅ Var | ✅ Var |
| Kişisel mesaj üretme | ❌ Yok | ✅ Var |
| Gelecek tahmini | ❌ Yok | ✅ Var |
| Uygulama boyutuna etkisi | ❌ +30–80 MB | ✅ Sıfır (API) |
| Güncelleme gerektirme | ❌ Sürekli | ✅ Otomatik |
| Bağlam anlama | ❌ Kelime bazlı | ✅ Cümle/paragraf bazlı |
| Entegrasyon kolaylığı | ⚠️ Orta | ✅ Basit HTTP isteği |

---

## 🔗 Teknik Entegrasyon

Claude API, uygulamaya **OkHttp** HTTP istemcisi ile entegre edildi. API anahtarı güvenlik için `local.properties` dosyasında saklanıyor ve `BuildConfig` üzerinden okunuyor — bu sayede kaynak koduna gömülmüyor.

```
Uygulama (Kotlin)
      ↓ OkHttp ile HTTPS isteği
Anthropic API (https://api.anthropic.com/v1/messages)
      ↓ JSON yanıt
ClaudeService.kt → parse eder → ViewModel'e iletir
      ↓
Kullanıcı ekranda sonucu görür
```

Kullanılan model: **claude-opus-4-6**

---

## 💡 Özet

> MindLyric, kullanıcının ruh halini anlamak için Claude API'yi tercih etti çünkü:
>
> ✔ Türkçe'yi doğru anlıyor
> ✔ Sadece sınıflandırmak değil, **anlamak ve yanıt üretmek** için tasarlandı
> ✔ Uygulamayı şişirmiyor
> ✔ Skor + öneri + tahmin gibi **3 farklı görevi** tek bir entegrasyonla karşılıyor
> ✔ Kullanıcıya sayı değil, **empati** sunuyor
