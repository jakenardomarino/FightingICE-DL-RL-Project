import json
import pandas as pd
import numpy as np
import os
import glob

# --- CONFIGURATION ---
STAGE_WIDTH = 960.0
STAGE_HEIGHT = 640.0
MAX_HP = 400.0
MAX_ENERGY = 300.0
MAX_ROUND_FRAMES = 3600.0
MAX_MOVE_FRAMES = 60.0 

def get_one_hot_state(prefix, state_str):
    """ One-hot encodes character state (STAND, CROUCH, AIR, DOWN). """
    possible_states = ['STAND', 'CROUCH', 'AIR', 'DOWN']
    features = {}
    for s in possible_states:
        key = f"{prefix}_state_{s}"
        features[key] = 1.0 if state_str == s else 0.0
    return features

def get_action_category(action_str):
    """ Categorizes raw action strings into broader types. """
    if not action_str: return "NEUTRAL"
    action_str = action_str.upper()
    
    if "GUARD" in action_str: return "GUARD"
    if "DASH" in action_str or "WALK" in action_str: return "MOVE"
    if "JUMP" in action_str: return "JUMP"
    if "THROW" in action_str: return "THROW"
    if "DAMAGE" in action_str or "RISE" in action_str: return "HIT"
    if "FIREBALL" in action_str or "D_DF_FA" in action_str: return "PROJECTILE"
    if "_A" in action_str or "_B" in action_str or "KICK" in action_str: return "ATTACK"
    
    return "NEUTRAL"

def get_one_hot_action(prefix, action_str):
    """ One-hot encodes action categories. """
    category = get_action_category(action_str)
    possible_actions = ["NEUTRAL", "GUARD", "MOVE", "JUMP", "THROW", "HIT", "PROJECTILE", "ATTACK"]
    
    features = {}
    for act in possible_actions:
        key = f"{prefix}_act_{act}"
        features[key] = 1.0 if category == act else 0.0
    return features

def get_one_hot_keys(prefix, player_data):
    """ Extracts key presses (A, B, C, Up, Down, Left, Right) as binary features. """
    keys = ['key_a', 'key_b', 'key_c', 'key_up', 'key_down', 'key_left', 'key_right']
    features = {}
    for k in keys:
        # JSON boolean to float (1.0 or 0.0)
        val = 1.0 if player_data.get(k, False) else 0.0
        features[f"{prefix}_{k}"] = val
    return features

def calculate_projectile_threat(my_x, projectiles):
    """ Calculates normalized threat level based on projectile distance. """
    if not projectiles: return 0.0
    min_dist = 9999.0
    found = False
    
    for proj in projectiles:
        try:
            pos = proj.get('current_pos', {})
            p_x = pos.get('x', -999)
            dist = abs(my_x - p_x)
            if dist < min_dist:
                min_dist = dist
                found = True
        except: continue

    if found and min_dist < 500:
        return (500.0 - min_dist) / 500.0
    return 0.0

def parse_replay_files(file_list, label, filter_losses_only=False):
    valid_frames = []
    total_rounds = 0
    kept_rounds = 0

    for filename in file_list:
        print(f"Processing {filename}...")
        if not os.path.exists(filename): continue

        try:
            with open(filename, 'r') as f:
                data = json.load(f)
        except: continue

        raw_rounds = data.get('rounds', [])
        total_rounds += len(raw_rounds)

        for round_frames in raw_rounds:
            if not round_frames: continue

            # Filter Logic (Losses Only)
            if filter_losses_only:
                last_frame = round_frames[-1]
                p1_hp = last_frame['P1']['hp']
                p2_hp = last_frame['P2']['hp']
                if p1_hp >= p2_hp: continue

            kept_rounds += 1
            
            for frame in round_frames:
                if frame['current_frame'] < 10: continue

                p1 = frame.get('P1', {})
                p2 = frame.get('P2', {})

                # --- 1. BASIC STATUS ---
                hp_diff = (p1.get('hp', 0) - p2.get('hp', 0)) / MAX_HP
                en_diff = (p1.get('energy', 0) - p2.get('energy', 0)) / MAX_ENERGY
                remaining_time = frame.get('remaining_frames', 0) / MAX_ROUND_FRAMES

                # --- 2. POSITION & ORIENTATION ---
                p1_x = p1.get('x', 0)
                p2_x = p2.get('x', 0)
                p1_facing = 1.0 if p1.get('front', True) else -1.0
                
                dist_x = abs(p1_x - p2_x) / STAGE_WIDTH
                dist_y = abs(p1.get('y', 0) - p2.get('y', 0)) / STAGE_HEIGHT
                
                if p1_facing > 0: wall_dist = p1_x / STAGE_WIDTH
                else: wall_dist = (STAGE_WIDTH - p1_x) / STAGE_WIDTH

                # --- 3. PHYSICS ---
                raw_spd_x = p1.get('speed_x', 0)
                dir_to_opp = 1.0 if p2_x > p1_x else -1.0
                my_closing_speed = (raw_spd_x * dir_to_opp) / 20.0
                
                opp_raw_spd_x = p2.get('speed_x', 0)
                opp_dir_to_me = -dir_to_opp
                opp_closing_speed = (opp_raw_spd_x * opp_dir_to_me) / 20.0
                
                my_speed_y = p1.get('speed_y', 0) / 20.0
                opp_speed_y = p2.get('speed_y', 0) / 20.0

                # --- 4. ONE-HOT FEATURES ---
                my_state_feats = get_one_hot_state('my', p1.get('state', 'STAND'))
                opp_state_feats = get_one_hot_state('opp', p2.get('state', 'STAND'))
                
                my_action_feats = get_one_hot_action('my', p1.get('action', 'STAND'))
                opp_action_feats = get_one_hot_action('opp', p2.get('action', 'STAND'))
                
                # [NEW] Key Press Inputs
                my_key_feats = get_one_hot_keys('my', p1)
                opp_key_feats = get_one_hot_keys('opp', p2)

                # --- 5. VULNERABILITY ---
                p1_rem = p1.get('remaining_frames', 0)
                p2_rem = p2.get('remaining_frames', 0)
                frame_advantage = (p2_rem - p1_rem) / MAX_MOVE_FRAMES
                
                # --- 6. PROJECTILE LOGIC ---
                proj_threat = calculate_projectile_threat(p1_x, p2.get('projectiles', []))
                my_proj_threat = calculate_projectile_threat(p2_x, p1.get('projectiles', []))

                # --- ASSEMBLE ROW ---
                row = {
                    'hp_diff': hp_diff,
                    'en_diff': en_diff,
                    'dist_x': dist_x,
                    'dist_y': dist_y,
                    'wall_dist': wall_dist,
                    'my_closing_speed': my_closing_speed,
                    'my_speed_y': my_speed_y,
                    'opp_closing_speed': opp_closing_speed,
                    'opp_speed_y': opp_speed_y,
                    'frame_advantage': frame_advantage,
                    'proj_threat': proj_threat,
                    'my_proj_threat': my_proj_threat,
                    'remaining_time': remaining_time,
                    'label': label
                }
                
                # Add all One-Hot Dictionaries
                row.update(my_state_feats)
                row.update(opp_state_feats)
                row.update(my_action_feats)
                row.update(opp_action_feats)
                row.update(my_key_feats)  # New!
                row.update(opp_key_feats) # New!

                valid_frames.append(row)

    print(f"Processed {len(file_list)} files.")
    print(f"Total Rounds: {total_rounds} -> Kept: {kept_rounds}")
    return pd.DataFrame(valid_frames)

if __name__ == "__main__":
    expert_files = glob.glob("data/*BlackMamba*.json")
    negative_files = glob.glob("data/*TOVOR*.json")
    
    print(f"Found {len(expert_files)} Expert files and {len(negative_files)} Negative files.")

    df_expert = parse_replay_files(expert_files, label=1, filter_losses_only=False)
    df_negative = parse_replay_files(negative_files, label=0, filter_losses_only=True)

    if not df_expert.empty or not df_negative.empty:
        full_df = pd.concat([df_expert, df_negative], ignore_index=True)
        full_df = full_df.sample(frac=1).reset_index(drop=True)
        
        output_filename = "training_data_v3_full.csv"
        full_df.to_csv(output_filename, index=False)
        
        print("\n" + "="*30)
        print(f"DONE! Saved to: {output_filename}")
        print(f"Total Rows: {len(full_df)}")
        print(f"Total Features: {len(full_df.columns) - 1}") 
        print("="*30)
    else:
        print("\nError: No data extracted.")