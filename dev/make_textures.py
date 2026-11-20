"""Draws Workbench Wisdom's textures. Run from the repo root: python3 dev/make_textures.py
Writes the Reforging Bench block faces (16x16) and its screen (176x166 on a 256x256 sheet)."""
from PIL import Image

B = 'src/main/resources/assets/workbenchwisdom/textures/'
WOOD = [(46, 30, 14), (62, 41, 20), (79, 53, 27), (96, 66, 35), (112, 79, 43)]
IRON = [(38, 38, 42), (62, 62, 67), (88, 88, 94), (118, 118, 124), (150, 150, 156), (190, 190, 196)]


def new(w=16, h=16):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


def planks(img, y0, y1, seed):
    """Horizontal planks, 4 px tall each, staggered seams, a little grain."""
    px = img.load()
    for y in range(y0, y1):
        row = (y - y0) // 4
        for x in range(16):
            c = WOOD[2] if (x * 7 + row * 3 + seed) % 11 else WOOD[3]
            if (y - y0) % 4 == 3:
                c = WOOD[1]                     # the gap under each plank
            elif (x + row * 5 + seed) % 8 == 0 and (y - y0) % 4 != 0:
                c = WOOD[1]                     # a seam between boards
            elif (y - y0) % 4 == 0:
                c = WOOD[3] if c == WOOD[2] else WOOD[4]
            px[x, y] = c


def iron_band(img, y0, y1):
    px = img.load()
    for y in range(y0, y1):
        for x in range(16):
            px[x, y] = IRON[4] if y == y0 else IRON[2] if y < y1 - 1 else IRON[1]
    for x in (1, 14):
        px[x, y0 + (1 if y1 - y0 > 2 else 0)] = IRON[5]


def legs(img):
    """The bench stands on four legs: the middle of the bottom is open (dark under the top)."""
    px = img.load()
    for y in range(12, 16):
        for x in range(16):
            if 3 <= x <= 12:
                px[x, y] = WOOD[0] if y > 12 else WOOD[1]
            else:
                px[x, y] = WOOD[2] if x in (0, 15) else WOOD[3] if x in (1, 14) else WOOD[1]
    for x in range(16):
        px[x, 11] = WOOD[1]


# side: the iron top's edge, two planks of apron, the legs
side = new()
planks(side, 3, 11, 0)
iron_band(side, 0, 3)
legs(side)
side.save(B + 'block/reforging_bench_side.png')

# front: the same with a hammer and tongs hung on the apron
front = side.copy()
fp = front.load()
for (x, y) in ((3, 4), (4, 4), (5, 4), (3, 5), (4, 5), (5, 5)):
    fp[x, y] = IRON[3] if y == 4 else IRON[1]
for y in range(6, 11):
    fp[4, y] = WOOD[4] if y % 2 else WOOD[3]
for (x, y) in ((10, 4), (12, 4), (10, 5), (12, 5), (11, 6), (11, 7), (10, 8), (12, 8), (10, 9), (12, 9), (10, 10), (12, 10)):
    fp[x, y] = IRON[2] if y < 7 else IRON[1]
fp[11, 6] = IRON[4]
front.save(B + 'block/reforging_bench_front.png')

# top: a steel working plate in a wooden rim, rivets in the corners, worn bright in the middle
top = new()
tp = top.load()
for y in range(16):
    for x in range(16):
        if x in (0, 15) or y in (0, 15):
            tp[x, y] = WOOD[2] if (x + y) % 5 else WOOD[3]
        else:
            tp[x, y] = IRON[2]
for i in range(1, 15):
    tp[i, 1] = IRON[4]
    tp[1, i] = IRON[3]
    tp[i, 14] = IRON[1]
    tp[14, i] = IRON[1]
for y in range(5, 11):
    for x in range(4, 12):
        tp[x, y] = IRON[3] if (x + y) % 3 else IRON[4]
for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
    tp[x, y] = IRON[5]
top.save(B + 'block/reforging_bench_top.png')

# bottom: plain planks
bottom = new()
planks(bottom, 0, 16, 3)
bottom.save(B + 'block/reforging_bench_bottom.png')

# ------------------------------------------------------------------ the screen, vanilla style
W, H = 176, 166
gui = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
g = gui.load()
BG, LIGHT, DARK, LINE = (198, 198, 198), (255, 255, 255), (85, 85, 85), (0, 0, 0)
SLOT, SLOT_DARK = (139, 139, 139), (55, 55, 55)
for y in range(H):
    for x in range(W):
        corner = (x in (0, W - 1) and y in (0, 1, H - 2, H - 1)) or (y in (0, H - 1) and x in (0, 1, W - 2, W - 1))
        if corner:
            continue
        if x == 0 or y == 0 or x == W - 1 or y == H - 1:
            g[x, y] = LINE
        elif x <= 2 or y <= 2:
            g[x, y] = LIGHT if not (x >= W - 3 or y >= H - 3) else BG
        elif x >= W - 3 or y >= H - 3:
            g[x, y] = DARK
        else:
            g[x, y] = BG
# round the bevel corners a touch
for (x, y) in ((1, 1), (W - 2, H - 2)):
    g[x, y] = LINE


def slot(x, y, size=18):
    for j in range(size):
        for i in range(size):
            c = SLOT
            if i == 0 or j == 0:
                c = SLOT_DARK
            if i == size - 1 or j == size - 1:
                c = LIGHT
            if (i == 0 and j == size - 1) or (j == 0 and i == size - 1):
                c = SLOT
            g[x + i, y + j] = c


slot(26, 46)
slot(75, 46)
slot(133, 46)
# a plus between the item and what's added
for k in range(-4, 5):
    g[59 + k, 55] = DARK
    g[59, 55 + k] = DARK
    g[59 + k, 54] = SLOT_DARK if abs(k) < 5 else DARK
# an arrow to the result
for x in range(102, 116):
    for y in range(52, 58):
        g[x, y] = SLOT
for k in range(8):
    for y in range(55 - (7 - k), 55 + (7 - k)):
        g[116 + k, y] = SLOT
for x in range(102, 124):
    for y in range(47, 63):
        if g[x, y] == SLOT and (g[x, y - 1] == BG or g[x - 1, y] == BG):
            g[x, y] = DARK
# player inventory
for r in range(3):
    for c in range(9):
        slot(7 + c * 18, 83 + r * 18)
for c in range(9):
    slot(7 + c * 18, 141)
gui.save(B + 'gui/reforging_bench.png')
print('textures written')
