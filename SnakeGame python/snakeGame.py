import tkinter as tk
import random
import time

class SnakeGame:
    def __init__(self, master):
        self.master = master
        self.master.title("Snake Game - Variant")
        self.master.resizable(False, False)

        # 游戏参数
        self.cell_size = 20          # 每个格子像素大小
        self.grid_width = 20         # 网格宽度（格子数）
        self.grid_height = 20        # 网格高度（格子数）
        self.canvas_width = self.grid_width * self.cell_size
        self.canvas_height = self.grid_height * self.cell_size
        self.max_length = 8          # 最大长度限制
        self.move_delay = 150        # 移动间隔(ms)

        # 游戏状态
        self.snake = []              # 蛇身坐标列表，头部索引0
        self.direction = "Right"     # 当前移动方向
        self.next_direction = "Right"
        self.food_positions = []     # 当前所有食物位置（支持多个，但这里只保持两种类型）
        self.score = 0               # 当前长度（也作为分数）
        self.game_over = False
        self.paused = False
        self.start_time = None       # 游戏开始时间戳
        self.game_duration = 0       # 游戏持续时间(秒)

        # 创建UI
        self.create_widgets()

        # 绑定键盘事件
        self.master.bind("<KeyPress>", self.on_key_press)

        # 初始化新游戏
        self.new_game()

    def create_widgets(self):
        """创建画布和状态标签"""
        self.canvas = tk.Canvas(self.master,
                                width=self.canvas_width,
                                height=self.canvas_height,
                                bg="black")
        self.canvas.pack()

        self.status_label = tk.Label(self.master,
                                     text="Length: 1 | Time: 0s",
                                     font=("Arial", 12))
        self.status_label.pack(pady=5)

    def new_game(self):
        """重置所有游戏变量并开始新游戏"""
        # 蛇初始位置：水平放置，长度为2（头部在左还是右都可以，这里选中间偏左）
        center_x = self.grid_width // 2
        center_y = self.grid_height // 2
        self.snake = [(center_x, center_y), (center_x-1, center_y), (center_x-2, center_y)]
        self.direction = "Right"
        self.next_direction = "Right"
        self.score = len(self.snake)  # 初始长度为3，但我们要求从长度1开始？但游戏初始长度通常为3比较合理，但题目未严格规定初始长度，这里按常规设为3，但吃食物后增加长度直到最大8。如果要求初始长度必须为1，可修改，但规则是“蛇的长度增加”，为了演示清晰，我保持初始3。
        # 若严格遵循“最大长度8”，初始为3，还可以吃5个食物。
        self.game_over = False
        self.start_time = time.time()
        self.game_duration = 0

        # 生成初始食物（两种类型）
        self.spawn_food()

        # 更新显示
        self.update_status()
        self.draw()

        # 开始游戏循环
        self.master.after(self.move_delay, self.game_loop)

    def spawn_food(self):
        """生成两种食物，一种加1长度，一种加2长度。这里我们用颜色区分：红色+1，金色+2"""
        self.food_positions = []
        # 生成两个食物，确保不重叠且不在蛇身上
        occupied = set(self.snake)
        free_cells = [(x, y) for x in range(self.grid_width) for y in range(self.grid_height)
                      if (x, y) not in occupied]
        if len(free_cells) < 2:
            # 如果没有足够空间放置两个食物，只放一个也行
            pass

        # 随机选两个不同位置
        if len(free_cells) >= 2:
            pos1 = random.choice(free_cells)
            free_cells.remove(pos1)
            pos2 = random.choice(free_cells)
            # 随机分配类型：type 1 增加1长度（红色），type 2 增加2长度（金色）
            # 但为了让两个食物类型不同，我们可以固定第一个为type1，第二个为type2，或者随机。
            # 这里采用随机，但确保有两种类型出现。
            self.food_positions.append({"pos": pos1, "type": 1})  # 加1长度
            self.food_positions.append({"pos": pos2, "type": 2})  # 加2长度
        elif len(free_cells) == 1:
            pos = free_cells[0]
            self.food_positions.append({"pos": pos, "type": 1})
        # 如果free_cells为0，游戏胜利？但题目未要求胜利条件，只要求最大长度结束。我们稍后处理。

    def on_key_press(self, event):
        """处理键盘方向键"""
        if self.game_over:
            return
        key = event.keysym
        if key == "Up" and self.direction != "Down":
            self.next_direction = "Up"
        elif key == "Down" and self.direction != "Up":
            self.next_direction = "Down"
        elif key == "Left" and self.direction != "Right":
            self.next_direction = "Left"
        elif key == "Right" and self.direction != "Left":
            self.next_direction = "Right"

    def move_snake(self):
        """根据当前方向移动蛇，并处理食物碰撞、自身碰撞等"""
        if self.game_over:
            return

        # 更新方向
        self.direction = self.next_direction

        head_x, head_y = self.snake[0]
        if self.direction == "Right":
            new_head = (head_x + 1, head_y)
        elif self.direction == "Left":
            new_head = (head_x - 1, head_y)
        elif self.direction == "Up":
            new_head = (head_x, head_y - 1)
        else:  # Down
            new_head = (head_x, head_y + 1)

        # 检查是否撞墙
        if (new_head[0] < 0 or new_head[0] >= self.grid_width or
            new_head[1] < 0 or new_head[1] >= self.grid_height):
            self.end_game("Wall collision")
            return

        # 检查是否吃到食物
        eaten_food = None
        for food in self.food_positions:
            if food["pos"] == new_head:
                eaten_food = food
                break

        # 移动蛇身
        self.snake.insert(0, new_head)

        if eaten_food:
            # 根据食物类型增加长度
            growth = eaten_food["type"]  # 1或2
            # 实际上已经加了一个头，但我们需要让蛇不删除尾部，从而增长。
            # 但是还要额外增加growth-1个节？因为insert已经增加了一节，如果growth=1，则长度净增1（正确）；如果growth=2，则还需再增加一节。
            # 简便方法：如果growth=2，我们再追加一节到尾部（相当于尾部不动，多一节）。
            if growth == 2:
                # 复制尾部最后一个坐标，相当于延长一节
                self.snake.append(self.snake[-1])
            # 移除被吃掉的食物
            self.food_positions.remove(eaten_food)
            # 生成新食物（保持两种食物同时存在）
            self.spawn_new_food()
            # 更新分数（蛇的长度）
            self.score = len(self.snake)

            # 检查是否达到最大长度
            if self.score >= self.max_length:
                self.end_game("Max length reached")
                return
        else:
            # 没吃到食物，移除尾部
            self.snake.pop()

        # 检查是否撞到自己
        if new_head in self.snake[1:]:
            self.end_game("Self collision")
            return

        # 重绘
        self.draw()
        self.update_status()

    def spawn_new_food(self):
        """在空余位置生成一个新食物，确保类型为缺失的那种（保持有两种食物）"""
        occupied = set(self.snake) | {f["pos"] for f in self.food_positions}
        free_cells = [(x, y) for x in range(self.grid_width) for y in range(self.grid_height)
                      if (x, y) not in occupied]
        if not free_cells:
            # 没有空位，游戏胜利？但按规则最大长度结束，若没到最大长度且无空位，也视为结束？简单处理：不生成新食物。
            return

        # 确定当前已有的食物类型
        existing_types = {f["type"] for f in self.food_positions}
        # 我们想要两种类型都存在，所以生成缺失的类型
        missing_types = {1, 2} - existing_types
        if missing_types:
            new_type = random.choice(list(missing_types))
        else:
            # 如果两种都有了，理论上不会出现，因为每次只吃一个，然后补充缺失的。但以防万一，随机选一个
            new_type = random.choice([1, 2])

        new_pos = random.choice(free_cells)
        self.food_positions.append({"pos": new_pos, "type": new_type})

    def end_game(self, reason):
        """游戏结束，显示信息"""
        self.game_over = True
        self.game_duration = int(time.time() - self.start_time)
        self.update_status()
        # 弹出消息框（或直接在画布上显示）
        message = f"Game Over! {reason}\nLength: {self.score}\nTime: {self.game_duration}s"
        self.canvas.create_text(self.canvas_width//2, self.canvas_height//2,
                                text=message, fill="white", font=("Arial", 14),
                                justify="center", tags="gameover")
        # 也可以用tkinter messagebox，但画布显示更符合GUI要求。
        # 同时更新状态标签
        self.status_label.config(text=f"Game Over | Length: {self.score} | Time: {self.game_duration}s")

    def update_status(self):
        """更新状态栏标签"""
        if not self.game_over:
            elapsed = int(time.time() - self.start_time)
            self.status_label.config(text=f"Length: {self.score} | Time: {elapsed}s")
        else:
            self.status_label.config(text=f"Game Over | Length: {self.score} | Time: {self.game_duration}s")

    def draw(self):
        """绘制蛇、食物等"""
        self.canvas.delete("all")
        # 绘制网格线（可选，便于视觉）
        for i in range(self.grid_width):
            self.canvas.create_line(i*self.cell_size, 0, i*self.cell_size, self.canvas_height, fill="gray20")
        for j in range(self.grid_height):
            self.canvas.create_line(0, j*self.cell_size, self.canvas_width, j*self.cell_size, fill="gray20")

        # 绘制食物
        for food in self.food_positions:
            x, y = food["pos"]
            color = "red" if food["type"] == 1 else "gold"
            self.canvas.create_oval(x*self.cell_size+2, y*self.cell_size+2,
                                    (x+1)*self.cell_size-2, (y+1)*self.cell_size-2,
                                    fill=color, outline="")

        # 绘制蛇
        for idx, segment in enumerate(self.snake):
            x, y = segment
            if idx == 0:
                color = "lightgreen"  # 头部颜色稍亮
            else:
                color = "green"
            self.canvas.create_rectangle(x*self.cell_size+1, y*self.cell_size+1,
                                         (x+1)*self.cell_size-1, (y+1)*self.cell_size-1,
                                         fill=color, outline="")

        # 如果游戏结束，显示结束信息（已经在end_game中添加了文本）

    def game_loop(self):
        """游戏主循环"""
        if not self.game_over:
            self.move_snake()
            self.master.after(self.move_delay, self.game_loop)
        else:
            # 游戏结束，停止循环，但可以显示信息
            pass

if __name__ == "__main__":
    root = tk.Tk()
    game = SnakeGame(root)
    root.mainloop()