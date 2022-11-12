display.set_font("sans")
display.set_pen(BLACK)
display.clear()
display.set_pen(WHITE)
display.rectangle(1, 1, 137, 41)
value = 4373
expand = ""
if value < 1000:
    expand = " "
display.set_pen(BLACK)
display.text(expand+str(value), 7, 23, wordwrap=100, scale=1.1) # format does not work correctly
display.text(expand+str(value), 8, 23, wordwrap=100, scale=1.1)

display.text("W", 104, 23, wordwrap=100, scale=1.1)
display.text("W", 105, 23, wordwrap=100, scale=1.1)
display.update()
