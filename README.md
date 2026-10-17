# Ashin & Flower

เกมแอ็กชันผจญภัย 2D มุมมองจากด้านบน ธีม Dark Fantasy เขียนด้วย Java OOP และ Java Swing
ผู้เล่นควบคุม Ashin นักเดินทางที่หลงเข้ามาในดินแดนที่ถูกวิญญาณยึดครอง ต้องใช้ Long Sword ต่อสู้กับวิญญาณ
และเก็บดอกไม้เพื่อฟื้น HP ระหว่างทาง ผ่าน 3 ด่านไปจนถึง Spirit Ruins เพื่อเผชิญหน้ากับ SpiritGuardian

## ดาวน์โหลดเกม

ดาวน์โหลด `AshinAndFlower.jar` ได้จากหน้า [Releases](https://github.com/maMai007/Ashin-And-Flower/releases) ของ repo นี้
ต้องติดตั้ง **Java 17 ขึ้นไป** แล้วดับเบิลคลิกไฟล์เพื่อเล่นได้เลย

ถ้าแก้โค้ดแล้วอยากสร้าง jar ใหม่ ให้รัน `AshinAndFlower/build-jar.bat` ไฟล์ที่ได้จะอยู่ที่ `AshinAndFlower/dist/AshinAndFlower.jar`

## วิธีรัน

ต้องใช้ **JDK 17 ขึ้นไป**

รันจากโฟลเดอร์ `AshinAndFlower`:

```
cd AshinAndFlower
javac -encoding UTF-8 -d bin src/ashinandflower/*.java
java -cp bin ashinandflower.Game
```

หรือเปิดโฟลเดอร์ของ repo ใน VS Code (ติดตั้ง Extension Pack for Java) แล้วกด Run ได้เลย

## วิธีเล่น

| ปุ่ม | การกระทำ |
| --- | --- |
| `W` `A` `S` `D` | เดิน |
| คลิกเมาส์ | ฟันดาบไปทางที่ Ashin หันหน้าอยู่ |

เป้าหมายของแต่ละด่าน:

- **ด่าน 1 — Forgotten Garden:** กำจัด WanderingGhost ให้หมด เก็บดอกไม้เพื่อฟื้น HP ได้
- **ด่าน 2 — Haunted Forest:** กำจัด WanderingGhost และ ShadowGhost ให้หมด เก็บดอกไม้เพื่อฟื้น HP ได้
- **ด่าน 3 — Spirit Ruins:** เอาชนะบอส SpiritGuardian (ด่านนี้ไม่มีดอกไม้ฟื้น HP) แล้วเก็บ Spirit Flower ที่ปรากฏขึ้นเพื่อชนะเกม

ถ้า HP หมดจะ Game Over และเลือกเล่นด่านเดิมใหม่ (Retry) หรือกลับเมนูหลักได้

## โครงสร้างโฟลเดอร์

```
AshinAndFlower/
├── src/   ซอร์สโค้ด Java ของเกม (package ashinandflower)
└── res/   ไฟล์ประกอบเกม: รูปภาพ (images), ฟอนต์ (fonts), เสียง (audio)
```

## Credits

- ฟอนต์ Press Start 2P — SIL Open Font License (ดู `res/fonts/OFL.txt`)
- เพลง — "Cinematic Ambient ,Bart.wav" โดย BassmanJourney\
  https://freesound.org/people/BassmanJourney/sounds/632109/\
  License: Creative Commons 0 (CC0)
