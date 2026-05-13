# MindLyric — Dosya Yapısı Rehberi

---

## 📱 EKRANLAR (UI Katmanı)

Bu klasördeki dosyalar kullanıcının gördüğü ekranları yönetir.

---

### 🔐 ui/auth/ — Giriş & Kayıt Ekranları

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `LoginActivity.kt` | Giriş ekranının kodları. Email/şifre okur, ViewModel'e gönderir, sonuca göre ana ekrana geçer. |
| `RegisterActivity.kt` | Kayıt ekranının kodları. Kullanıcı adı, email, şifre alır, ViewModel'e gönderir. |
| `AuthViewModel.kt` | Giriş ve kayıt işlemlerinin iş mantığı. Alanları doğrular, DB'ye yazar, sonucu ekrana bildirir. |
| `AuthViewModelFactory.kt` | AuthViewModel'i oluşturmak için gereken yardımcı sınıf. (Android zorunluluğu) |

---

### 📓 ui/journal/ — Günlük Ekranı

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `JournalViewModel.kt` | Günlük ekleme, silme ve listeleme işlemlerini yönetir. Claude API'yi çağırarak duygu analizi yaptırır. |
| `JournalViewModelFactory.kt` | JournalViewModel'i userId ile birlikte oluşturan yardımcı sınıf. |
| `JournalAdapter.kt` | Günlük listesini ekranda gösterir. Her kartı (tarih, metin, skor, öneri) doldurur. |

---

### 👤 ui/profile/ — Profil Ekranı

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `ProfileActivity.kt` | Profil ekranının kodları. Avatar seçimi, şifre değiştirme, hesap silme, trend grafiği ve yarın tahmini gösterilir. |
| `ProfileViewModel.kt` | Profil ekranının iş mantığı. Kullanıcı bilgilerini, istatistikleri, grafiği ve tahmini yükler. |
| `ProfileViewModelFactory.kt` | ProfileViewModel'i oluşturan yardımcı sınıf. |

---

### 🏠 MainActivity.kt

Ana günlük ekranı. Kullanıcının günlüklerini listeler, yeni günlük eklenmesini sağlar, profil ikonuna tıklanınca profil sayfasına geçer.

---

## 🗄️ VERİTABANI (Data Katmanı)

Bu klasördeki dosyalar veritabanı işlemlerini yönetir. Kullanıcı hiçbirini görmez, arka planda çalışır.

---

### 📋 data/local/entity/ — Tablo Tanımları

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `UserEntity.kt` | Kullanıcı tablosunu tanımlar. id, kullanıcı adı, email, şifre (hash), avatar alanları var. |
| `JournalEntryEntity.kt` | Günlük tablosunu tanımlar. id, userId, tarih, metin, ruh hali, duygu skoru, öneri alanları var. |

---

### 🔍 data/local/dao/ — Veritabanı Sorguları

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `UserDao.kt` | Kullanıcı tablosuna sorgu atar. Kayıt, giriş, avatar güncelleme, şifre değiştirme, silme işlemleri burada. |
| `JournalDao.kt` | Günlük tablosuna sorgu atar. Ekleme, silme, listeleme, skor güncelleme, ortalama hesaplama işlemleri burada. |

---

### 🏛️ data/local/db/ — Veritabanı Kurulumu

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `MindLyricDatabase.kt` | Room veritabanının ana sınıfı. Hangi tablolar var, versiyon kaçıncı gibi bilgiler burada. (Şu an v5) |
| `DbProvider.kt` | Veritabanına tek noktadan erişim sağlar. "Singleton" — uygulama boyunca tek bir DB bağlantısı olur. |

---

### 🔧 data/local/util/ — Yardımcı Araçlar

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `HashUtil.kt` | Şifreleri SHA-256 algoritmasıyla şifreler. Düz metin şifre hiçbir zaman veritabanına kaydedilmez. |

---

### 📦 data/repository/ — İş Mantığı Köprüsü

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `UserRepository.kt` | ViewModel ile UserDao arasındaki köprü. Kayıt, giriş, avatar, şifre, hesap silme işlemlerini yönetir. |
| `JournalRepository.kt` | ViewModel ile JournalDao arasındaki köprü. Günlük CRUD işlemlerini ve istatistik sorgularını yönetir. |

---

### 🌐 data/remote/ — İnternet Bağlantısı

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `ClaudeService.kt` | Claude AI API'sine HTTP isteği gönderir. 3 işlev: duygu skoru analizi, kişisel öneri üretme, yarın tahmini. |

---

## 🎨 ARAYÜZ DOSYALARI (res/)

---

### 📐 res/layout/ — Ekran Tasarımları (XML)

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `activity_login.xml` | Giriş ekranının görsel tasarımı. |
| `activity_register.xml` | Kayıt ekranının görsel tasarımı. |
| `activity_main.xml` | Ana günlük ekranının görsel tasarımı. |
| `activity_profile.xml` | Profil ekranının görsel tasarımı. |
| `item_journal_entry.xml` | Listedeki her günlük kartının tasarımı. |
| `dialog_avatar_picker.xml` | Avatar seçim dialogunun tasarımı. |
| `dialog_change_password.xml` | Şifre değiştirme dialogunun tasarımı. |
| `item_avatar.xml` | Avatar grid'indeki tek bir avatarın tasarımı. |

---

### 🎨 res/values/ — Sabit Değerler

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `colors.xml` | Uygulamada kullanılan tüm renkler burada tanımlı. |
| `strings.xml` | Uygulamadaki tüm yazılar burada. Türkçe metinler buradan okunur. |
| `themes.xml` | Uygulamanın genel görünüm teması. |

---

### 🖼️ res/drawable/ — Görseller & Şekiller

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `avatar_1.jpg ... avatar_20.jpg` | 20 adet kullanıcı avatarı görseli. |
| `journal.png` | Uygulama logosu. |
| `ic_google.xml` | Google ikonu (vektör). |
| `ic_mail.xml` | Mail ikonu (vektör). |
| `bg_auth_gradient.xml` | Giriş ekranı radial gradient arka planı. |
| `bg_rounded_top.xml` | Üst köşeleri yuvarlak beyaz kart şekli. |
| `bg_recommendation.xml` | Günlük kartındaki öneri kutucuğunun arka planı. |
| `ellipse.png` | Giriş ekranındaki içbükey yay geçiş görseli. |

---

### 🔤 res/font/

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `thondpachem_regular.ttf` | Uygulamada kullanılan özel yazı tipi. |

---

## ⚙️ PROJE AYAR DOSYALARI

| Dosya | Ne İşe Yarıyor? |
|-------|----------------|
| `app/build.gradle.kts` | Uygulama bağımlılıkları (Room, OkHttp, MPAndroidChart vb.) ve derleme ayarları burada. |
| `AndroidManifest.xml` | Uygulamanın tanıtım kartı. Hangi ekranlar var, hangi izinler gerekli (internet gibi) burada yazar. |
| `local.properties` | Gizli ayarlar — Claude API anahtarı burada saklanır. Git'e gönderilmez. |
| `gradle/libs.versions.toml` | Tüm kütüphanelerin versiyon numaraları tek bir yerde yönetilir. |

---

## 🔄 MİMARİ AKIŞ (MVVM)

```
Kullanıcı
   ↓ tıklar
Activity / Fragment  (ekranı gösterir, tıklamaları dinler)
   ↓ iletir
ViewModel            (iş mantığını yönetir, DB'yi çağırır)
   ↓ kullanır
Repository           (DAO ile konuşur, API'yi çağırır)
   ↓ sorgular
Room DAO             (SQL sorgularını çalıştırır)
   ↓ okur/yazar
SQLite Veritabanı    (telefonda saklanan veri)
```
